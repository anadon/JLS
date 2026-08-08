# Issue #865: TASK-C583-2: the sponsor question is answered by name or answered "none found, searched here" — and a dated go/no-go lands in-tree, with "no, with reasons" being a complete answer
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of what this issue actually asks for

The decision half of a two-task pair under FEAT-C34-5 (#583, itself PF-5 of
CAP-34 / #518): #864 (TASK-C583-1) is the investigation — ITP process,
Policy obligations, cost arithmetic — and #865 is the verdict, gated on a
Debian sponsor existing by name (per KC-34-2 in #518, quoted verbatim in
this issue's AC-3). Both #864 and #865 are open, filed the same minute
(2026-08-04T15:38), each banded 0.5-1 mw, and neither has landed a document
yet. The chain (#518 → #583 → #864/#865) is internally consistent: I
verified KC-34-2's exact wording in #518 ("PF-5's ITP proceeds only if a
Debian sponsor materializes; self-NMU maintenance at bus factor 1 is refused
by name") matches how #865's AC-3 cites it, and #583's own AC-3/AC-4 map
cleanly onto what #865 is scoped to produce. The dependency graph and
boundary-setting here are sound; the acceptance criteria's teeth are not.

## Findings, most severe first

### 1. AC-1's "search" has no floor, and the budget structurally rewards a shallow one (HIGH)
AC-1: "answered by name, or answered 'none found' with the specific venues
and dates searched recorded." No minimum venue list, no minimum outreach
window, and no requirement that anyone was actually asked is specified —
"venues and dates searched" is satisfied by browsing two pages for ten
minutes and recording that. Finding a real Debian sponsor is a community
process (posting to debian-mentors@lists.debian.org or filing on
mentors.debian.net, then waiting for a Debian Developer to notice and
volunteer) that routinely takes weeks to months, not something achievable
inside a 0.5-1 maintainer-week band. As written, the AC and the budget
jointly create a strong incentive to satisfy the letter ("here's where I
looked") without ever making a genuine ask, which then predetermines the
"no, with reasons" outcome the issue frames as legitimate — except the
reasons would be "we didn't wait long enough to know," not "no sponsor
exists." Recommendation: name a minimum bar (e.g., a dated post to
debian-mentors and a stated minimum response window, or an explicit note
that the search was passive/directory-only if a live ask wasn't made) so a
future reader can tell "genuinely searched and got silence" from "looked
briefly and moved on."

### 2. AC-2 requires citing the cost arithmetic, not requiring the decision to follow from it (MEDIUM)
"AC-2: A dated go/no-go decision is committed in-tree with its reasons,
citing TASK-C583-1's cost arithmetic." Citation is not the same as
derivation — a decision can quote #864's maintainer-week numbers as
decoration while the actual verdict was reached on other (unstated)
grounds, and AC-2 as worded would still pass. Given AC-3 already makes "go"
conditional on a named sponsor, the arithmetic mostly matters for "no"
verdicts reached despite a sponsor existing (cost too high even with help)
or for calibrating what "steady-state cost" the decision text should weigh.
Recommendation: require the decision text to state explicitly how the cited
number changed (or didn't change) the verdict, not just that it was cited.

### 3. Naming a real, uninvolved person in-tree with no stated consent step (MEDIUM)
AC-1's "answered by name" means a specific individual's identity is
recorded permanently in this public repository's history as the
rationale-bearing subject of a go decision. Nothing in #865 (or #864, or
#518/#583) mentions confirming with that person that they're comfortable
being named this way before the commit lands — distinct from asking them to
sponsor, which is a separate step. A volunteer who tentatively agreed to
review a package could reasonably not expect to be committed into project
history by name as the linchpin of a distribution decision. Recommendation:
add a line to AC-1 requiring the named sponsor's confirmation to be named
(or default to role/venue rather than personal identity if that confirmation
wasn't obtained).

### 4. "Closes #583" is claimed unilaterally by a task that owns only half of #583's ACs (LOW)
"A recorded 'no, with reasons' closes #583 as completed." But #583's own
AC-1 and AC-2 (the ITP process write-up and the maintainer-week cost
derivation) are #864's scope, not #865's — #865 only satisfies #583's AC-3
through AC-5. The `ordering_after` field makes #864 finish first in
principle, but #865's closing claim doesn't condition on #864's document
having actually landed and satisfied its own ACs; it should say so
explicitly rather than relying on scheduling order alone. Recommendation:
make the closing clause conditional — "closes #583 provided #864's
document is already committed and satisfies its ACs" — instead of implying
#865 alone can retire the parent feature.

### One thing that is solid
The core design decision — that "no, with reasons" is a complete, closeable
answer, and that a "go" verdict is gated on a named sponsor rather than on
enthusiasm (AC-3, correctly citing KC-34-2) — is exactly right for a
bus-factor-1 project; ARCHITECTURE.md's own recorded decisions (e.g. the
i18n non-goal) independently confirm "single-maintainer pedagogy tool" is
this project's honest self-description, so refusing an unsponsored ITP is
consistent with the codebase's own stated constraints, not an invented
pretext. AC-4 (stating checkable conditions that would flip the decision)
is also well-specified and not gameable as written — keep both as-is.
