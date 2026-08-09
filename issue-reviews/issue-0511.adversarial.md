# Issue #511: CAP-27: a prospective user goes from first hearing of JLS to a running, understood example circuit in under ten minutes — without reading anything longer than a caption
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

CAP-27 is a capstone-tier tracking issue for JLS's onboarding gap. Its
factual premises hold up against the checked-out tree (verified
independently below), and — unusually for this kind of umbrella issue —
all six planned features (PF-1..PF-6) have actually been filed
(#545/#548/#550/#551/#552/#553) and further decomposed into tasks. The
problems are not with the diagnosis; they are with the acceptance
criteria's testability, one direct internal contradiction between an AC
and a kill-criterion, an ownership gap on the issue's own headline metric,
and an evidence citation that cannot be verified from the branch a normal
contributor would check out.

## Findings, most severe first

**1. (High) AC-2 — the capstone's one quantitative claim — has no owner, and a downstream feature already assumes it exists.**
AC-2 reads: *"A scripted fresh-user protocol (documented, re-runnable)
measures install→running-example in <10 minutes on Windows, macOS,
Linux."* Comment #1 (the coverage pass) explicitly logs this as a gap:
*"AC-2 fresh-user <10-min protocol — GAP-NOTED... owned by no single
feature; belongs to CAP-27 close-out, not a seventh feature."* Comment #2,
posted four days later, raises the identical question again ("Does #73's
usability trial belong here?") and again defers it: *"This pass
deliberately does not choose... flagged not decided."* Meanwhile #552
(FEAT-C27-5, PF-5) AC-2 already reads *"verified by the capstone's
scripted fresh-user protocol"* — a filed, in-flight feature presupposes an
artifact that, as of the latest comment on #511, still has no owner and no
filed task. If work starts on #552 before this is resolved, its AC-2 is
unverifiable by construction.
**Recommendation:** before any PF-5 work lands, either (a) file the
protocol as a task under the feature that owns the end-to-end path (as
comment #2 itself recommends) and record the `REPLAN:`, or (b) drop AC-2
from #511 to an aspirational note and let #73's n=5 IC1 trial (which
already has this exact ten-minute bound, see #73 §5) stand in as the
capstone-level evidence, with an explicit cross-reference.

**2. (High) AC-5 and KC-27-2 contradict each other.**
AC-5: *"Lesson 1 is completable by following on-screen prompts only."*
KC-27-2: *"If PF-5's lesson tooling exceeds its band, ship lessons as docs
pages and cut the in-tool variant — the outcome survives."* #552's own
AC-5 makes the in-tool assumption explicit: *"lessons are entered
deliberately (from the welcome pane or Examples menu); the default
editing view gains no new chrome."* A docs-pages-only lesson is by
definition not "on-screen prompts" in that sense. If PF-5 is cut to docs
per the kill criterion, AC-5 as literally worded cannot be satisfied — yet
KC-27-2 asserts the capstone's outcome is unaffected. One of the two must
be wrong: either AC-5 needs "or an equivalent written walkthrough," or
KC-27-2's "the outcome survives" claim is false and should say so.
**Recommendation:** reword AC-5 to be medium-agnostic ("...without
consulting material outside the lesson itself") so the kill criterion's
claim is actually true regardless of which variant ships.

**3. (Medium-High) `ordering_after: []` claims "startable now," but the same paragraph names a real blocking dependency it doesn't encode.**
The machine block says `ordering_after: []   # startable now; PF-3 touches
SimpleEditor and must respect the #84/FEAT-008 decomposition boundary`.
That is a contradiction inside one line: if PF-3 must respect an
in-flight architectural boundary owned by an open, unlanded issue, the
capstone is not unconditionally startable — the constraint has just been
demoted from a structured field (which other issues' automation reads)
to a prose aside. This is not paperwork pedantry here: #84 (open) is
actively targeting `SimpleEditor`'s exact 9-state mouse machine
(`enum State {idle, chosen, placing, moving, selecting, selected, option,
startwire, drawire}`, `src/jls/edit/SimpleEditor.java:770`) for extraction
into GoF State objects, including a rewrite of `setState` and its
dispatch. PF-3's first-run/welcome-pane work (#550, #770, #771) will need
to hook into whatever `EditWindow` state entry point exists — building it
against pre-#84 `SimpleEditor` risks a rebase collision on the exact
surface #84 is mid-refactoring.
**Recommendation:** either add `84` (or its parent `316`) to
`ordering_after`, or add an explicit written note on #550/#770/#771 that
PF-3's welcome-pane hook must be re-derived if #84 lands first — don't
leave this as an un-encoded aside on the capstone.

**4. (Medium) The cited evidence file is unreachable from the branch a contributor actually has checked out.**
The yaml's `evidence` field cites
`docs/reviews/evidence/2026-08-niche-survey/jls-baseline-adversarial-check.md`.
That path does not exist anywhere in this checkout (`find . -iname
"*niche-survey*"` and `find . -iname "*evidence*"` under `docs/` both
return nothing). Per #510's own body, that directory was "committed... on
`claude/jls-project-review-505pnf`" — a branch that does not exist in this
repository's branch list (`git branch -a` shows only `master` and the
current session branch). The specific factual claims the citation backs
(2/5 learning-on-ramp score, empty `JTabbedPane` first launch, zero
README screenshots, no discoverable examples) all independently
re-verify against the current tree — I checked README.md (one badge, no
`![`), `src/jls/` (no `welcome`/`firstRun` symbol), and `examples/`
(only `autograde/`, no user-facing circuits) — so the diagnosis is not
wrong. But the citation as written cannot be followed by anyone working
from `master`, which defeats the purpose of citing it.
**Recommendation:** either merge/preserve that evidence directory on a
reachable ref, or cite the specific facts inline (as #73 and #381 already
do with `grep`/`ls` transcripts) instead of a path that only exists on a
throwaway review branch.

**5. (Medium) AC-3 quietly drops a requirement PF-2's own prose promises, and the drop has already propagated downstream.**
PF-2 (the planned-features list) promises circuits ship "each with a
caption and a suggested exercise." AC-3, the actual gate, only requires
"each loads, simulates, and carries a caption" — the "suggested exercise"
half is absent from the acceptance criterion that actually closes the
capstone. This isn't hypothetical drift: #766 (TASK-C548-2), the task
that will do the work, mirrors AC-3's weaker wording exactly (AC-1: "at
least ten curated circuits... each carries a caption," no exercise
clause), while the "fails the build on missing exercise" enforcement is
attributed to a different task, #768, per #381's ownership table. If #768
is ever cut or delayed independently of #766, AC-3 as worded on #511 can
close (and the capstone can be marked done) without a single suggested
exercise existing — contradicting PF-2's stated promise.
**Recommendation:** restate AC-3 to require the exercise field, or
explicitly note in #511 that "suggested exercise" is PF-2 flavor text, not
a gate — currently it reads as a requirement but isn't graded as one.

**6. (Medium) Undeclared cross-capstone dependency, acknowledged but unresolved as of the latest comment.**
Comment #3 documents that #579 (Flathub, AC-4) and #580 (winget, AC-4)
both name CAP-27/#511 as the sole producer of a screenshot/description
set they consume and explicitly forbid commissioning a second set — yet
#511 declares no `blocks:` relationship to either, and there is no scope
item in #511 covering "produce store-listing-grade assets, not just
README-grade ones." The comment itself says three plausible owners exist
(#511 as hand-curated set / #519+#586 as a generated pipeline / a prose-vs-image
split) and "this pass deliberately does not choose." That's an honest
flag, but it means PF-1's deliverable shape is currently underspecified
in a way two other open features already depend on.
**Recommendation:** resolve via `REPLAN:` before PF-1 (#545) starts
producing images, since the format/resolution/count requirements differ
between a README embed and a Flathub/winget listing.

**7. (Low-Medium) AC-1 is not a testable predicate as stated on this issue.**
"README shows the product (screenshots + GIF) above the fold" has no
defined check at the #511 level — "above the fold" is meaningless for a
scrolling Markdown page rendered at arbitrary viewport widths, and no
automated drift check is named here (contrast #381's P4, which at least
asked for "at least two checked-in image paths that exist on disk," or
#760, which reportedly adds a drift-check-on-missing-image — but that
specificity lives on a child issue, not on #511's own AC-1). As worded, a
single small screenshot placed anywhere in the first few lines would
satisfy AC-1 by a literal reading, even if it doesn't function as a shop
window.
**Recommendation:** inherit the stronger, testable wording from #760/#762
into #511's AC-1 rather than leaving the capstone's own gate vaguer than
its child's.

**8. (Low) Band plausibility note.** The `band_mw: "9-15"` header sums
correctly against the six PF ranges (1-2 + 2-3 + 1-2 + 1-2 + 3-4 + 1-2 =
9-15), so the arithmetic is honest. Worth flagging only that PF-5 alone
accounts for up to 4 of the 15 mw (~27%) for the one feature whose own
kill criterion (KC-27-2) already anticipates being descoped to docs-only
— the band is soft at exactly the point most likely to be cut.

## What's solid (one line each)

- The "why this is a capstone" diagnosis reproduces independently against
  the current tree: README has exactly one badge and zero `![` images;
  `grep -rn "welcome\|firstRun" src/jls/` is empty; `resources/samples/`
  doesn't exist; `examples/` has only `autograde/`, no user-facing
  circuits — all match the issue's claims.
- Feature-coverage completeness is unusually good for a capstone: all six
  PFs are filed (#545/#548/#550/#551/#552/#553) and further decomposed
  into tasks (#760/#762/#764/#766/#768/#770/#771/#866), not left as
  placeholder bullets.
- KC-27-1's startup-time clause ("PF-3 is a per-commit gate on startup-time
  regression") is a genuinely falsifiable, CI-enforceable kill criterion —
  a good contrast to the vaguer "first-year conceptual load... unchanged"
  clause in the same kill criterion, which is not similarly testable but
  is at least explicitly delegated to K9/D9 (recorded elsewhere) rather
  than invented here.
- `ordering_after: []` being otherwise correct (no closed-issue references,
  no dangling numeric IDs) checks out — the one gap is the PF-3/#84
  interaction noted in finding 3, not a fabricated dependency.

## Verdict rationale

None of the findings above make the capstone's goal wrong or its
diagnosis unsound — the on-ramp problem is real and well-evidenced. But
AC-2 (finding 1) and the AC-5/KC-27-2 contradiction (finding 2) mean the
issue's own success criteria cannot currently be checked as written, and
finding 3's un-encoded ordering risk is a concrete collision waiting to
happen against #84. These are fixable by comment (`REPLAN:`), not by
redoing the diagnosis or the feature split — hence
**sound-with-concerns** rather than **needs-rework**.
