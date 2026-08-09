# Issue #555: FEAT-C28-2: `docs/performance.md` states JLS's throughput with the full method — hardware, JDK, flags, node counts — honestly framed, and the README cites it in one line
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

Create `docs/performance.md` (events/s, cycles/s, full methodology, honest
comparative framing against Digital) and a one-line README pointer to it,
sourced entirely from the committed benchmark suite that #554 (FEAT-C28-1)
is supposed to produce. Band: 0.5–1 maintainer-week.

## Findings, most severe first

**1. (High) The real dependency chain is far deeper than the issue's
`ordering_after` admits.** `ordering_after` names only `FEAT-C28-1 (#554)`.
But #554 itself hard-orders on `#413 TASK-0025`, which is `blocked_by:
[377, 379]` (TASK-0022/TASK-0023) — both of which are listed as "not
filed" in #335's own decomposition table, with a parent-feature cost band
of 5–10 maintainer-weeks for the chain #555 ultimately sits behind. A
contributor picking up #555 by reading only this issue would reasonably
believe it is one hop from ready; it is actually gated behind at least
four unfiled/unlanded predecessors. Recommendation: either state the full
transitive chain in the issue body, or mark #555 explicitly draft/blocked
rather than open-and-actionable.

**2. (High) AC-2's "stated tolerance" is unbounded and self-graded —
classic gameable acceptance criterion.** "An independent party can
reproduce each published number within a stated tolerance from the doc
alone" lets whoever writes `docs/performance.md` pick the tolerance. A
±50% (or looser) band trivially satisfies the letter of AC-2 while
defeating its purpose, and there is real variance to hide behind: the
existing (ad-hoc, pre-#554) numbers in
`docs/capability-roadmap/keystone-c-performance.md` already range from
~8,090 cycles/s (warm event loop) to ~1,100–1,450 cycles/s (end-to-end
CLI, JVM start included) for the *same* workload, a >5x spread depending
on what's being measured. Nothing in #555 requires the tolerance to be
derived from #554's actual run-to-run variance rather than picked to be
comfortable. Recommendation: pin a concrete tolerance (or require it be
derived from N reps of the committed suite) in the acceptance criterion
itself.

**3. (Medium) AC-3's "honest comparative framing" invites an
apples-to-oranges comparison the issue doesn't flag as such.** The only
competitor number in scope (Digital's self-reported "120 kHz simulated
processor clock on a 2012 i5", per #512) is external, self-reported, on
different hardware, and not measured under #555's own protocol; the
issue's dedup comment on this very thread concedes the rigorous
side-by-side belongs to #560, and that #555's AC-3 is "a sentence in this
doc, not a measurement programme." As written, AC-3 lets that
uncontrolled external figure sit next to JLS's rigorously-measured,
same-methodology numbers under one document's "honest" banner, with no
requirement to caveat that one side of the comparison wasn't measured the
same way. Recommendation: require the doc to explicitly label the
competitor figure as an unverified external claim pending #560.

**4. (Medium) AC-4's audit obligation ("no public performance claim
anywhere in README/docs that the doc does not back") is unscoped and
likely exceeds the stated 0.5–1 mw band.** `docs/capability-roadmap/keystone-c-performance.md`
already publishes performance numbers — "318 ns per event," "124 µs per
simulated clock cycle," "≈8,090 simulated CPU cycles per second" —
explicitly labeled "measured this session," i.e. an ad-hoc run, not
#554's committed suite. Taken literally, AC-4 requires either backing
those pre-existing figures from the new doc or getting that document
reclassified/disclaimed as historical-only; #555 names neither task nor
budgets for it. Recommendation: enumerate the pre-existing performance
prose #555 must reconcile (or explicitly exempt) as part of scoping AC-4.

**5. (Medium) None of the four ACs has an automated check.** This corpus's
own precedent (`CalibrationFixtureTest`, `NoRiscvDirectoryReferencesTest`,
`HeadlessCoreRatchetTest`) is to back every non-trivial claim with a
ratchet test; #555 proposes none. "Independent reproduction within
tolerance" and "no unbacked claim exists anywhere" are both manual/human
judgment calls that can pass review once and silently drift the moment
either doc is hand-edited later — and the issue itself defers the
staleness ratchet to a separately unfiled lane ("Keeping these numbers
from going stale is FEAT-C28-3 … not a manual process here"), so there is
a real window between #555 landing and FEAT-C28-3 landing where nothing
catches drift. Recommendation: at minimum, assert (via a simple test)
that the numbers in `docs/performance.md` match what #554's suite last
emitted, even before the full scheduled-lane ratchet exists.

**6. (Low) Cross-document consistency with `docs/machine-calibration.md`
(#335) is stated as a rule ("must not disagree on shared constants — cite,
don't fork") with no enforcement named.** Same class of gap as #5, lower
severity because #335 is the primary owner of that document and its own
drift risk.

**7. (Low) Process overhead disproportionate to the deliverable.** The
`feat_id` is flagged "provisional; renumbered by the adversarial phase,"
and this single doc-page-plus-one-line-README task carries a capstone
(#512), a sibling feature (#554), a three-issue dedup analysis comment,
and live cross-references to #335/#413/#442. Arguably justified given the
shared-constants risk (finding 6), but worth naming as coordination cost
against a 0.5–1 mw deliverable.

## What's solid

- AC-1's vocabulary (events/s, cycles/s, hardware/JDK/flags/node counts)
  isn't invented from scratch — it matches the methodology already
  demonstrated in `docs/capability-roadmap/keystone-c-performance.md`, so
  the doc has a real precedent to follow.
- The explicit ban on using an "ad-hoc run" and the requirement to source
  numbers only from #554's committed suite is a sound, self-aware guard
  against exactly the kind of drive-by benchmark that would undermine
  AC-2's reproducibility claim.
- The maintainer's own boundary comment on this thread proactively
  resolved apparent overlap with #560 and #588 (producer/citer chain,
  not duplication) before anyone had to ask — good due diligence.
- The parent capstone's KC-28-1 kill criterion ("publication proceeds
  anyway" even if JLS is >5x behind Digital) pre-empts the obvious gaming
  strategy of quietly dropping AC-3 to avoid an unfavorable number.
