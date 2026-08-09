# Issue #785: TASK-C591-1: a venue, a deadline, format limits and a fallback venue are written down, so the submission is a plan rather than an intention
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This is a small planning task (band_mw 0.25-0.5) nested under #591 (submit a
paper on JLS's grading contract to a peer-reviewed venue), itself gated on
#589 (the white paper) and #509 (the WashU course pilot). Nothing in the
repository — no `docs/whitepapers/`, no file matching `TASK-C591`,
`FEAT-C36`, or `grading-contract` — exists yet to ground this task; it is
pure process scaffolding for work that hasn't started. The core idea (name
a venue and deadline before drafting) is sound project hygiene, but the
acceptance criteria as written are easy to satisfy on paper while leaving
the real risks — deadline feasibility, human-subjects ethics, and a broken
cross-reference to its own successor task — completely unaddressed.

## Findings, most severe first

1. **Missing human-subjects/ethics gate for course-experience data — a real
   compliance hazard, not mentioned anywhere in the chain.** #591's AC-2
   says the draft covers "what happened when a real course ran on it," and
   #509 is a live pilot with WashU's Dr. Siever involving actual students
   (CSE 260M). Academic venues (SIGCSE and ASEE explicitly; WCAE by
   extension of SIGCSE policy) require IRB/ethics approval or an explicit
   ethics statement for any paper reporting on student data or course
   outcomes — this is standard SIGCSE/ASEE submission policy, not an
   edge case. Issue #785's AC list (name venue, deadline, page limits,
   fallback) never asks whether IRB approval or exemption is a prerequisite
   for the *chosen* venue, and neither #591 nor #509 mentions IRB at all.
   A venue could be picked, a deadline tracked, and the draft (#786)
   completed, only to discover at submission time (TASK-C591-3) that the
   course-experience section cannot legally/ethically be submitted without
   approval that was never sought. **Recommendation:** add an AC to #785
   requiring the note to state whether the chosen venue requires an ethics
   statement/IRB approval for the course-experience material, and if so,
   who owns obtaining it and by when — before the draft is written, not
   after.

2. **The venue is a category, not a choice — contradicts the issue's own
   framing.** The title asserts "a venue... are written down" (singular),
   but the body only ever says "SIGCSE / WCAE / ASEE class" (`## Outcome`:
   *"names the target venue (SIGCSE / WCAE / ASEE class)..."*). These are
   not interchangeable: SIGCSE Technical Symposium papers run ~6-10 pages
   in ACM format with a ~August deadline for a March conference roughly
   seven months later; WCAE is a one-day workshop co-located with SIGCSE
   with its own (later, rolling) CFP and short-paper format; ASEE annual
   conference papers run to a different cycle (~fall deadline, full paper
   ~10-15 pages, different reviewing culture entirely). AC-1 as worded
   ("A tracked note... names the target venue, its deadline, and its
   page/format limits") can be satisfied by naming all three as "the
   target" with three different deadlines and calling that done, which
   defeats the stated purpose (a single deadline the rest of #591 is
   "scheduled against"). **Recommendation:** require the note to commit to
   exactly one primary venue with one deadline; the other two collapse
   into the fallback slot the issue already asks for.

3. **Deadline feasibility is asserted, never checked, against the issue's
   own dependency chain — the AC is gameable by construction.** #785 is
   `ordering_after: [589]`; #786 (the draft) is
   `ordering_after: [TASK-C591-1, 589, 588]`; #591 itself is
   `ordering_after` #520 PF-1, PF-2, and #509. That is a chain of at least
   four unstarted feature-tier issues (#589, #588, #509's course pilot,
   #520's PF-1/PF-2) before a draft can even begin, each with its own
   unknown timeline. #785's AC never requires the chosen deadline to be
   checked against this chain's realistic completion date — a reviewer
   could write "SIGCSE 2027, deadline August 2026" today and technically
   satisfy every checkbox, even though the venue's deadline has already
   passed (or will pass) long before #786's prerequisites (#589, #588,
   #509) are done. AC-4 ("dated and updated when the venue's terms
   change") is the only guard against staleness, but it has no defined
   revisit cadence or owner, so a note written once and never revisited
   again satisfies the letter of the AC while the substance (a deadline
   the project can actually hit) silently rots. **Recommendation:** add an
   explicit feasibility check — the note must state the deadline is at
   least N weeks after the realistic completion of #786's prerequisites,
   or must be revisited at a stated cadence (e.g., monthly) until
   submission.

4. **The cross-reference to TASK-C591-3 does not resolve to any filed
   issue.** AC-3 requires the note to state "which sections of the paper
   depend on #509's course pilot, so the fallback decision in
   TASK-C591-3 has a defined trigger date" — but TASK-C591-3 is never
   given an issue number anywhere in #785, #591, or #786 (which is
   TASK-C591-2, #786). I checked the neighboring issue numbers (#787 is
   TASK-C570-1, an unrelated feature) and there is no discoverable
   TASK-C591-3 issue in the tracker as of this review. An acceptance
   criterion that hands off a "trigger date" to a task that cannot be
   located cannot be verified as satisfied — there is nothing to check the
   handoff against. **Recommendation:** either file TASK-C591-3 before or
   alongside #785, or drop the forward reference until it exists.

5. **"Tracked note in tree or on #591" is an either/or with no
   reconciliation, inviting the two-copies-diverge failure mode.** AC-1
   allows the note to live "in tree or on #591" — a GitHub issue comment
   and a committed file are different artifacts with different edit/audit
   histories. If a comment is later contradicted by a differently-worded
   in-tree note (or vice versa), nothing in #785 says which one is
   authoritative, and #786/#591 downstream consumers have no way to know
   which to read. **Recommendation:** pick one location as canonical (a
   committed file is the better choice, since AC-4 requires it to be
   "dated and updated," which git history verifies for free) and let the
   other, if used at all, merely link to it.

6. **`ordering_after: [589]` is asserted without justification, and may be
   backwards.** Choosing a venue's page/format limits arguably should
   happen independently of whether #589's white paper is finished — the
   causal link (why must the white paper exist before a venue can be
   picked?) is never stated. If the real reason is "we need to know the
   paper's likely length/scope to pick a venue with matching page limits,"
   that's a reasonable rationale, but it's not written down, and it means
   this "planning artifact only" task (per the Boundary section) is
   nonetheless hard-blocked on a substantial unstarted feature-tier
   deliverable. **Recommendation:** state the rationale for the ordering
   explicitly, or decouple: a tentative venue/deadline note can be drafted
   in parallel with #589 and firmed up once page-limit-relevant scope is
   known.

## What's solid

- The core discipline — force a dated, falsifiable venue/deadline/fallback
  note before drafting starts, rather than letting "we should submit
  somewhere" drift indefinitely — is genuinely good practice and matches
  the project's existing convention of dated research notes under `docs/`
  (e.g. `docs/flatlaf-evaluation-2026-07.md`,
  `docs/picocli-evaluation-2026-07.md`).
- The Boundary section cleanly separates this task from drafting
  (TASK-C591-2/#786) and submission (TASK-C591-3), which avoids scope
  creep into writing the paper itself.
- Requiring the CFP to be cited (AC-1) is a good anti-gaming measure on its
  own — it forces a real, checkable source rather than a paraphrase.

## Verdict rationale

The planning discipline is worth keeping, but two findings are load-bearing
enough to block starting as written: the missing IRB/ethics gate (#1) is a
real compliance risk once #509's student data enters the picture, and the
unresolved TASK-C591-3 reference (#4) makes AC-3 unverifiable today. The
venue-as-category ambiguity (#2) and the unchecked deadline-vs-dependency-chain
feasibility gap (#3) mean the acceptance criteria can be checked off without
producing the thing the issue actually wants (a deadline the project can hit).
Recommend reworking the AC list before work starts.
