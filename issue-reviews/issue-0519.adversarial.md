# Issue #519: CAP-35: one documentation source builds the in-app help, a hosted versioned manual, and the screenshots — and none of it can silently drift from the program it describes
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

#519 is a well-grounded capstone: its evidence quote about ARCHITECTURE.md's
orphaned "hosted docs are the planned future" decision checks out verbatim
(ARCHITECTURE.md:252-267), the existing drift-proofing it builds on
(`HelpTopicsTest`, `HotkeysHelpAccuracyTest`) is real and substantial (350
and 383 lines respectively, verified), and the capstone correctly identifies
a genuine gap: no site-generation infrastructure exists in this repo today
(no `gh-pages` workflow, no `maven-site`/Jekyll config beyond a Jacoco
report). But the issue has a structural problem that dominates everything
else below, plus several soft/gameable acceptance criteria.

## Findings, most severe first

**1. The issue is already functionally superseded by its own children, but no formal link exists, and #519 stays open with unclear closing criteria.**
The single comment on #519 (2026-08-04) reports that all four planned
features were filed as separate issues — PF-1→#584, PF-2→#585, PF-3→#586,
PF-4→#587 — and maps every one of #519's five acceptance criteria onto
those children's ACs ("Carried as that issue's AC-1..AC-4", etc.). I fetched
all four and confirmed each declares `serves_capstones: [519]` and restates
the corresponding AC set nearly verbatim. Yet `issue_read get` on #519
reports `"has_children":false` and `get_sub_issues` returns `[]` — the
GitHub sub-issue relationship was never actually created, only asserted in
prose. Anyone opening #519 without reading the one comment (or without
`get_sub_issues` tooling) will see five acceptance criteria with no
indication that all the work is meant to happen in four *other* numbered
issues. Risk: duplicate/conflicting implementation (e.g., someone picks
Markdown for the source format against #519 while another PR targets
AsciiDoc against #584), or #519 sitting open forever after all four children
close because nobody defined "close #519 when #584-587 close" as an
explicit rule.
**Recommendation:** use `sub_issue_write` to formally link #584-587 as
sub-issues of #519 now, and add an explicit closing rule to #519's body
("close when all four children close, no independent PRs against this
issue number").

**2. The issue body contains a claim its own follow-up comment flags as stale, left uncorrected by policy.**
The YAML `related` field says: `"existing HelpTopicsTest link-checker (#70)
is the seed of the drift-proofing discipline"`, phrased as if #70 is a live,
ongoing effort. The comment on the same issue states plainly: `"related"
row cites #70 as a live seed; #70 is closed (landed as the HelpTopicsTest
link checker). Not a defect... recording it so no one chases a closed issue
expecting open work.` and then explicitly declines to fix the body ("Left
uncorrected here by design — this run does not edit existing issue bodies
(per #489)"). A reader who trusts the issue body over the comment will
misjudge #70 as open, actionable, related work. Deliberately shipping a
known-inaccurate issue body is a process smell worth pushing back on even
if it is policy-compliant.
**Recommendation:** at minimum, edit the body once a maintainer (not the
filing automation) touches the issue, or add a pinned correction.

**3. `ordering_after: []` is asserted but contradicted by the issue's own follow-up analysis.**
The YAML block states `ordering_after: []` for #519, implying no
prerequisite work gates it. The comment on this very issue says: "PF-3's
`ordering_after: []` understates PF-3's real dependency" — #101 (the
Wayland/X11 screenshot rig PF-3/#586 is supposed to reuse) is itself open
with two unresolved weaknesses that would silently degrade a capture
pipeline built on it: `PIXEL_DIFF_MIN` is still `"0"` (record-only — I
confirmed this against #101's body: "a blank frame currently passes"), and
the promoted `gui-wayland` lane fail-opens on JBR CDN download failure
(confirmed: #101 §"Honest limits" says exactly this). A capstone's own
machine-readable dependency field being wrong, by its own admission, one
comment later, is exactly the kind of internal contradiction this lens is
supposed to catch.
**Recommendation:** correct `ordering_after` to name #101's outstanding
close-out tasks, or explicitly record the risk as accepted with a named
compensating check (as #586 eventually does — but #519 itself does not).

**4. AC-3 and CAP-27's PF-1/PF-4 make overlapping ownership claims over the same artifact (README screenshots) without a crisp arbiter.**
#519 AC-3: "Screenshots in README/docs are build products, not
hand-pasted files." CAP-27 (#511) PF-1: "README shop window... screenshots,
an animated GIF" and PF-4: "Circuit gallery page... linked from README."
The `related` field's stated boundary ("CAP-27 owns the first-ten-minutes
surfaces, CAP-35 owns the documentation SYSTEM") sounds clean but doesn't
resolve who is accountable if the README screenshots are stale: is it a
CAP-27 failure (wrong content chosen) or a CAP-35 failure (pipeline
didn't regenerate them)? #586 (PF-3's actual filing) does note CAP-27
"consumes" the output — a good sign the delta was thought through at the
child level — but #519's own AC-3 as written claims the outcome
("screenshots ... are build products") without deferring the README's
content ownership to #511, so a strict reading lets either capstone claim
or disclaim AC-3 depending on which is more convenient at review time.
**Recommendation:** narrow AC-3 in #519's body to "the pipeline exists and
produces images on demand," and let #511 own "README shows them."

**5. AC-5's "or better" and KC-35-1's "priced before chosen" are unfalsifiable escape hatches.**
AC-5: "In-app help renders the same content tree it renders today or
better — no regression for the offline lab machine." "Better" has no
metric, so a migration that changes formatting, drops a page under a
different name, or reflows content differently can always be defended as
"better" rather than regressed, defeating the no-regression intent. KC-35-1:
"If the in-jar target forces the source format below usefulness ... the
in-app viewer upgrades ... rather than the source degrading — priced before
chosen." Priced by whom, against what threshold, reviewed by whom? As
written, no PR can ever be shown to have violated this criterion — there is
no falsifiable trigger.
**Recommendation:** AC-5 should say "byte-identical or diffed-and-approved
per AC-5 of #584" (which does define a byte-auditable diff report — good,
but #519 itself doesn't inherit that rigor in its own AC-5 text). KC-35-1
should name who signs off on the "priced" estimate before the tradeoff is
taken.

**6. Hosted manual (PF-2/#585) is a materially larger and riskier lift than the "1-2 mw" band implies, with no existing hosting infrastructure to build on.**
I verified there is no `gh-pages` or `Pages`-related CI workflow
(`.github/workflows/*.yml` grep came back empty) and no `maven-site`
plugin usage beyond a Jacoco report target. Standing up a new hosted,
versioned, per-release, search-enabled public site (#585's own AC-1..AC-5)
is not a documentation-content task — it is new infrastructure: a
publishing pipeline, a domain/hosting decision, release-time credentials,
and a link-integrity CI gate on external infrastructure JLS doesn't
currently operate. #519 does not surface this as a distinct risk (it is
folded into PF-2's "1-2 mw" estimate in the child issue, not flagged in the
capstone body at all). This is exactly the kind of scope/cost risk that
should be named at the capstone level, since it also creates a new
release-time step that AC-1 of #585 requires the release procedure itself
to grow ("cutting a release emits ... and repoints latest").
**Recommendation:** name the hosting decision (GitHub Pages vs. self-hosted
vs. Netlify) as a decision-needed item in #519 or #585 before costing it,
and flag the new release-time publishing step as a CI/credentials change
worth its own security review (deploy tokens, supply-chain surface).

**7. AC-4's rigor is undercut by its own comment's admission of a much larger uncovered gap, which #519 doesn't foreground.**
#519 AC-4: "A deliberately wrong doc claim (wrong hotkey, missing element
page) fails CI." Trivially satisfiable by one narrow planted check. The
comment on #519 itself identifies the real gap: `CliFlagTableTest` (#71)
"reads no documentation file at all... docs/batch-interface.md can name a
flag the parser rejects and nothing turns red" — called "the largest
genuinely uncovered gap under PF-4." #587 (PF-4's actual filing) does pick
this up as AC-1, which is good, but #519's own AC-4 text gives no signal
that flag-doc drift is the priority delta versus the two examples it names
(hotkey, missing page), both of which are *already* covered today by
existing tests per the same comment. A minimal-effort implementation could
satisfy #519's AC-4 literally while leaving the actual named gap (flags)
untouched.
**Recommendation:** rewrite AC-4 to explicitly require the flag-documentation
assertion, since that's the only genuinely new ground identified.

## What's solid (no further action needed)

- The evidence quote about ARCHITECTURE.md's orphaned "hosted docs" decision
  is accurate and well-sourced (ARCHITECTURE.md:252-267).
- The claim that existing drift-proofing (`HelpTopicsTest`,
  `HotkeysHelpAccuracyTest`) already covers topic-existence and
  hotkey-content checks is verified true against the actual test files —
  the capstone correctly builds on real, not imagined, infrastructure.
- The single-jar offline-completeness constraint is treated consistently
  across #519 and its children (repeated verbatim in #584 AC-3), which is
  the right amount of redundancy for a load-bearing invariant.
- band_mw arithmetic checks out: 2-3 + 1-2 + 2-3 + 1-2 sums to 6-10 mw,
  matching the capstone's own `band_mw: "6-10"`.

## Verdict rationale

Not `should-not-proceed` — the underlying gap is real, well-evidenced, and
already decomposed into four reasonably scoped child issues with tighter
acceptance criteria than the capstone itself. Not plain `sound` because of
the structural sub-issue-linkage gap (finding 1), the self-admitted stale
`ordering_after` (finding 3), and several acceptance criteria on #519
itself that are looser than the versions inherited by its children — a
reviewer working from #519 alone (rather than #584-587) would sign off on
a weaker bar than the project's own filed follow-through intends.
