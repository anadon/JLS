# Issue #509: Adoption target: WashU CSE 260M (bsiever/JLS fork) migrates to this fork — define "well enough matured" as written acceptance criteria
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the issue

Filed by the maintainer (anadon, OWNER), untiered by choice. Claims WashU's
CSE 260M course teaches on the `bsiever/JLS` fork, that Dr. Siever has been
contacted and is "interested... if it becomes well enough matured," and asks
this repo to (1) turn that phrase into written criteria ratified by Siever,
(2) audit the fork-delta against `bsiever/JLS`, (3) prove the CSE 260M lab
corpus loads/simulates/grades here, (4) verify installer coverage for the
course's platform mix, (5) state a release-channel policy, and ultimately
have "one course offering" run on this fork's releases. A same-day comment
thread already found and patched a sequencing bug between this issue and
#577.

## Findings, most severe first

**1. [Critical] The load-bearing factual claim has no verifiable evidence
attached, and AC-1 inherits the same weakness.** The entire issue rests on
one sentence: "**2026-08-04, maintainer:** Dr. Siever has been contacted and
is interested in this fork if it becomes well enough matured." There is no
linked email, no quoted message, no comment from a `bsiever` account, no
reference to a public thread — it is an unsourced first-person assertion by
the same person who filed the issue. Contrast this with the *other* factual
claims in the issue (course site URL, ACM CF'25 DOI 10.1145/3706594.3726971),
which are at least checkable by a third party. AC-1 then compounds the
problem: "A written criteria list exists in this issue, **confirmed by Dr.
Siever as sufficient for migration**" — but nothing in the issue specifies
what counts as "confirmed" (a quoted email? a GitHub comment from his
account? a maintainer's say-so, as with the original contact claim?). As
written, AC-1 can be closed exactly the way it was opened: by the maintainer
asserting it in a comment, with zero externally checkable artifact. That
makes the issue's central acceptance criterion gameable by construction.
**Recommendation:** require a specific, checkable evidentiary form for both
the original contact and AC-1's "confirmed" state — e.g., a comment from a
`bsiever`-controlled GitHub identity, or a linked/quoted email with a
verifiable header — before either is treated as satisfied.

**2. [High] AC-3 and #577 AC-1/AC-3 assign the same work to two issues with
inconsistent conditions, and #509's own body never surfaces the licensing
gate that governs it.** #509's AC-3 reads: "The full CSE 260M lab corpus
passes load + simulate + grade on a tagged release of this fork, byte-stable
across the course's platforms" — stated as if it's work this issue owns
outright, with no mention of needing permission to use WashU's course
materials. But #577 (filed the same day) makes content licensing an explicit
gate: "AC-3: Content licensing is settled in writing with Dr. Siever before
any adapted material ships; absent agreement, the fixtures land as
compatibility fixtures only." The 2026-08-08 correction comment on #509
itself concedes the two issues were filed with a circular `ordering_after`
(#509 blocked on #577's AC-1, which was itself declared satisfied by #509's
AC-3) and had to be patched post hoc into "#577 AC-1/AC-2 → #509 AC-3 → #509
AC-1/AC-4 → #577 AC-3/AC-4." That the graph was wrong on day one, on the
issue the author calls "queue item 0," is itself evidence the dependency
claims here should not be trusted without independent verification.
**Recommendation:** edit #509's AC-3 text directly to reference #577 AC-1 as
the discharging artifact (the comment says this should happen but the issue
body was never edited to match, the same "comment fixes it, body still lies"
pattern seen on #571) and state the licensing dependency in #509's own body
rather than leaving it locatable only by reading #577.

**3. [High] The corpus needed for AC-3 is not owned, controlled, or even
possessed by this project, and the issue treats obtaining it as a solved
step.** "Course-corpus compatibility" is listed as an almost-free warm-up
("this is a concrete, finite fixture corpus... so it is cheaper and should
come first") but the corpus consists of WashU's lab circuits, which live on
a course site and/or with the instructor, not in this repository or in
`bsiever/JLS`'s public tree (unverified — I could not confirm the lab files
are published anywhere public from what's cited). Actually obtaining a
complete, current corpus requires Dr. Siever's cooperation — the same
unconfirmed relationship finding 1 flags — so "should come first" and
"~free" undersell a dependency the issue's own #577 AC-3 later admits is
licensing-gated. Treating acquisition as trivial while treating permission-
to-ship as a separate, harder gate is an internal inconsistency: if you
don't have permission to ship the material, you likely don't have an
unambiguous right to even commit it as CI fixtures pending settlement.
**Recommendation:** state explicitly, in this issue, what license (if any)
covers use of the corpus for CI-only (non-shipping) purposes versus shipping,
and get that answer before committing fixtures anywhere.

**4. [Medium] AC-2's fork-delta audit cites facts about `bsiever/JLS` that
this review cannot verify and that are likely to be stale by the time work
starts.** "His fork has 13 open issues of its own" is asserted with no link
or date-stamp; issue counts on an actively-taught course's fork are exactly
the kind of number that drifts week to week. AC-2 demands "every bsiever/JLS
divergence and open issue has a recorded disposition" — an unbounded task
whose size depends entirely on how far the fork has drifted since it forked
from upstream JLS 4.1 (unknown from this repo's vantage point; could be a
handful of patches or a substantially rewritten codebase after ~years of
independent course-driven maintenance). The issue prices nothing for this
(no mw estimate anywhere, unlike sibling issues #577/#571 which carry
`band_mw` blocks) despite being filed by the same review process that
elsewhere insists on priced arithmetic (#508's "spot-audit machine-generated
arithmetic before filing" finding, and its "Filed untiered deliberately"
carve-out for #509 conveniently exempts it from that same rigor).
**Recommendation:** before starting the audit, get a live count of
`bsiever/JLS`'s open issues and diff size (commits/files changed since fork
point) and attach even a rough band, or explicitly declare the audit
unbounded and time-boxed instead.

**5. [Medium] AC-2's "port" disposition has an unaddressed license-
compatibility hazard.** The audit's three dispositions are
"already-fixed-here / port / decline-with-reason." Porting code from
`bsiever/JLS` assumes it is safe to merge into this GPL-3.0-or-later
codebase. README's own "License and provenance" section is unusually
careful about this project's chain of title (the 2014 MTU consent letter,
the note that it names GPLv3 without "or later," the fact the "or-later"
election is this project's own). Nothing in #509 asks whether `bsiever/JLS`
carries the same license, whether Siever's fork added any differently-
licensed dependencies, or whether he (or WashU) would need to be asked
before code attributed to him is redistributed under this project's terms.
Given the project's demonstrated sensitivity to provenance elsewhere, this
is a real gap, not a hypothetical one. **Recommendation:** add a
disposition precondition — confirm license compatibility per candidate
port, and treat "license unclear" as its own disposition rather than folding
it into "decline-with-reason" after the fact.

**6. [Medium] AC-4 ("one course offering runs on this fork's releases")
is calendar-dependent in a way the issue never states, and could sit open
for a year or more regardless of engineering effort.** The issue's own item
4 acknowledges "a course adopts a tool it can pin for a semester" — meaning
a mid-semester switch is implausible and the earliest realistic adoption
point is a future term boundary. Filed 2026-08-04, with WashU's cited
offering being Spring 2025 (already past), the next viable term is not
stated, is outside this project's control, and depends on university
scheduling and Siever's own course-planning timeline, not on anything this
tracker can accelerate once AC-1–AC-3 are done. Unlike the rest of the
project's issues, which measure cost in maintainer-weeks, this criterion's
critical path is measured in academic terms and a third party's calendar —
a scope/feasibility mismatch the issue doesn't call out.
**Recommendation:** split AC-4 into an engineering-controlled criterion
(e.g., "this fork is ready and Siever has committed to a specific term") and
a separate, explicitly out-of-project-control tracking note for the actual
term the migration happens, so the issue isn't left open for calendar
reasons that look like the same kind of engineering non-progress as
everything else in the tracker.

**7. [Low] "Byte-stable across the course's platforms" in AC-3 is ambiguous
given the project's own documented reproducibility caveats.** README states
plainly that installers are "*not* byte-reproducible" across builds
(wall-clock state embedded by native packaging tools), while the jar and BOM
are. AC-3's phrase doesn't specify whether "byte-stable" means installer
artifacts, saved `.jls` corpus files, or batch/grading output bytes
(`docs/batch-interface.md`'s stability contract, which is the plausible
intended referent). As written, a narrow reading (jar output only) and a
broad reading (installer bytes too, which the project's own docs say is
impossible) are both defensible, which makes this criterion's pass/fail
outcome dependent on which reading is chosen after the fact.
**Recommendation:** name the specific artifact(s) AC-3's byte-stability
claim is over (most likely: batch `-t`/grading stdout and VCD output,
consistent with `docs/batch-interface.md`), not "the course's platforms" in
the abstract.

**8. [Low] The issue exempts itself from the tracker's own governance and
then calls itself top priority, a tension it names but does not resolve.**
"Filed untiered deliberately: it should be promoted through the normal
capstone template process if the maintainer wants it in the tier system."
The 2026-08-08 comment concedes the cost: "the item the queue ranks first is
invisible to every `tier:capstone → tier:feature → tier:task` walk,
including the coverage passes that check whether each capstone's features
are complete." An issue that is simultaneously "queue item 0" and invisible
to the tooling that tracks whether queue items are progressing is a self-
inflicted risk of exactly the kind #508 flags project-wide (stranded
decisions, planning prose that doesn't reach master). The issue names this
risk but doesn't mitigate it (no milestone, no cross-link from #508's own
disposition table, which omits #509 entirely from its capstone table since
it is untiered).
**Recommendation:** at minimum, add #509 to a milestone or explicitly link
it from #508 so "untiered" doesn't become "untracked."

## What's solid

- Framing the audit-and-corpus work as cheap, source-available, and
  sequenced ahead of any Siever conversation is the right shape for a
  low-trust external dependency — cheap validation before asking someone
  for their time.
- Correctly identifies that a course wants a pinned release cadence (item
  4) rather than a feature list — this is a realistic, well-reasoned
  read of what an instructor actually needs.
- Explicitly marking the criteria "provisional until ratified with Dr.
  Siever" is the right epistemic posture for criteria authored by a party
  other than the actual stakeholder — the problem is execution (finding 1),
  not this framing choice.

## Verdict rationale

The instinct — find a real, named, external adoption target and work
backward to concrete criteria — is good process, and several structural
choices (cheap-audit-first sequencing, provisional criteria, calling out
release-cadence over features) are sound. But the issue's foundation is an
unverifiable private claim that its own top acceptance criterion (AC-1)
would close the same unverifiable way (finding 1); its AC-3 duplicates and
contradicts a licensing gate stated only on a sibling issue, a contradiction
the project's own review process had to patch within days of filing (finding
2); and AC-4 bakes in a calendar dependency the issue never names as such
(finding 6). These are fixable without abandoning the issue's goal, but they
need rework before AC-1 can be honestly marked done. Hence
**needs-rework**, not `sound-with-concerns`.
