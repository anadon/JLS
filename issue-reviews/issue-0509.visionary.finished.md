# Issue #509: Adoption target: WashU CSE 260M (bsiever/JLS fork) migrates to this fork — define "well enough matured" as written acceptance criteria
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the courtship and the target underneath is not "one instructor switches
repos." It is: **JLS stops being a family of mutually-illegible forks and
becomes one upstream with a defined downstream relationship.** anadon/JLS is a
4.1-lineage fork; bsiever/JLS is a 4.6–4.10-lineage fork; the tree already
carries the scar tissue of that split (`FileAbstractor.Container.PLAIN_TEXT`
exists *because* the 4.6–4.10 lineage dropped the XZ reader, #129;
`ShiftRegister` was reconstructed from `bsiever/JLS@038a5b67` because the fork
shipped an element this one lacked, #122). #509 is the first issue in the
tracker where an actual person on the other side of that split says "maybe."

That makes the issue important and its framing wrong in one specific way: it
treats reunification as a **decision another party makes once**, and prices the
project's work as preparation for that decision. Everything in the body flows
from that: an undefined maturity bar, a criteria conversation the project cannot
schedule, and two acceptance criteria (AC-1 "confirmed by Dr. Siever," AC-4 "one
course offering runs on this fork") that the project **cannot discharge by its
own action at any effort level**. An issue whose closure condition lives in
someone else's inbox is not a work item; it is a hope with a number.

## The reframing: be adoptable in pieces, not adopted as a whole

The instructor's word "matured" is almost certainly not a feature bar. An
instructor with a semester of 200 students at stake is pricing **the cost of
being stranded**: mid-term, on an unfamiliar binary, with files that no longer
open in the tool the syllabus names. "Mature" means *reversible*.

So invert the ask. Instead of "get good enough that he switches," build the
property that makes switching **not a commitment**:

> Any `.jls` file authored in either fork opens, simulates and grades
> identically in the other, and that is a tested, documented, standing
> guarantee — not an observation.

Under that guarantee CSE 260M does not need a migration event at all. It can
adopt this fork's *pieces* — the batch grading path, the signed installers, the
container image, VCD export — for whatever subset helps, keep bsiever/JLS for
the rest, and reverse any of it at zero cost. Adoption becomes a gradient
instead of a cliff, and the "conversation" stops being a negotiation and becomes
a demo.

This is not a new capability. It is the **naming and generalization of a
practice this repository has already performed four times ad hoc**: #122
(ShiftRegister ported from fork source, with a fixture written by *the fork's
own writer* — `test/fixtures/fork-4.6-shiftregister.jls`, now load-bearing in
`ShiftRegisterTest`, `DeterministicSaveTest`, `SimulationSeedOrderTest`,
`HeadlessCoreCanaryTest`, `CircuitOpTest` and `docs/vcd-interop.md`), #131
(JumpEndDialog, `bsiever/JLS@26053a00`), #121 (Trace), and `DeleteKeyPolicy`
(jwetzell 2016, bsiever 2024/25). The project has been doing fork-delta work for
a year without ever calling it a subsystem. #509 should be the issue that calls
it one.

## Concrete alternative design 1 — the dialect compatibility surface

Promote fork interop to a first-class, enforced surface alongside the two
contracts JLS already treats as normative (`docs/file-format.md`,
`docs/batch-interface.md`):

- `docs/fork-compat.md` — normative: which JLS dialects load here, which
  containers each lineage can read, which element tags exist in which lineage,
  and the standing bidirectional statement (`-savetext` is already the outbound
  half; the inbound half is the sniffing loader).
- `test/fixtures/fork-4.6/**` — the fixture pattern of #122 generalized: files
  written by *the other fork's writer*, one per divergent element, committed
  with provenance. A `fork-interop` CI lane loads/simulates/grades every one.
- A **fork-delta ledger** in the same document: every bsiever/JLS divergence and
  every one of his 13 open issues, with `already-fixed-here / ported (#N) /
  declined-because`. This is #509's item 1, but as a maintained artifact on
  master rather than a one-time audit table in an issue body — the difference
  matters, because the fork keeps moving and a table in an issue rots the day it
  is written.

Note what this fixes that nobody is currently watching: #488 records that JLS
today writes two element tags (`FieldExtend`, `RegisterFile`) that its own
frozen tag table and its own normative spec say do not exist. That is a live
**outbound** compatibility break — files this fork writes that no other JLS can
load — sitting under an issue whose title never mentions compatibility. A named
dialect surface gives that class of bug a home.

Architectural note: `docs/grand-architecture.md` partitions all open issues into
six layers over one triad and claims (§10) that "every open issue lands in
exactly one module with no forcing." #509 does not. Interop is neither a runtime
module nor really the distribution band; it is a *contract* band cutting across
`core` (persistence, tags), `batch` (the `-t` grammar and verdicts), and
distribution (release cadence). The grand architecture has no concept of a
sibling implementation of its own formats — which is a gap in the architecture
document, exposed by this issue, and worth one paragraph there.

## Concrete alternative design 2 — merge in the direction that costs him nothing

The issue assumes reunification flows toward anadon/JLS ("bsiever/JLS either
merges back or redirects to here"). That is the direction that maximizes the
counterparty's risk and minimizes ours, which is exactly backwards for the party
with zero adoption and bus factor 1 asking a favor of the party with the live
course.

Cheaper and more persuasive: **send PRs to bsiever/JLS.** His 13 open issues are,
as #509 correctly says, the most valuable requirements document this project has
ever had — so fix some of them, in his tree, on his lineage. Cost per fix is
small (this fork has the tests, the ratchets, and in several cases the fix
already). The effects compound in a way no maturity bar does: it demonstrates
maintenance capacity on his terms, it puts this project's code into the course's
running binary without any migration decision, and it makes the eventual "one
JLS" a merge between collaborators rather than a defection. If the goal is one
upstream, the arrow may point either way; the tracker should stop assuming it
points here.

## Concrete alternative design 3 — what #509 should actually own

After #577 takes the corpus (per the ordering-correction comment, correctly),
#517 takes course content, #502 takes platform kits, and #300/#306 take the
verdict engine, exactly one item in this issue body has **no owner anywhere in
the tracker** — and it is the one the issue itself calls "likely worth more to
an instructor than any feature":

> Item 4, release-channel stability: "a tagged release per academic term,
> patch-only within a term."

I searched the open tracker: there is no release-cadence or support-policy issue.
Zero. Meanwhile the roadmap contains a 12–17 mw four-platform grading kit (#502)
and a 9–14 mw course-content capstone (#517), both aimed at instructors who,
before any of that matters, need to know the tool will not move under them in
week 9.

So: **make #509 the release-and-support-policy issue.** A published `SUPPORT.md`
stating the term cadence, the patch-only-within-a-term rule, the support window
in terms, the pinning recipe (release tag, container digest, `bom.json`), and a
deprecation policy for the `.jls` dialect and the batch contract. It is
unilateral, it is ~1 mw, it is the single most instructor-legible artifact the
project can produce, and — decisively — **it is worth having even if Dr. Siever
never replies.** Every other item in this issue is contingent on a counterparty;
this one converts the counterparty's silence into a no-op.

## Disregarding the stated acceptance criteria

I am explicitly setting aside AC-1 and AC-4 as acceptance criteria for this
issue, and I do not think the ratification conversation rescues them.

- **AC-1** ("a criteria list confirmed by Dr. Siever as sufficient") makes issue
  closure a function of a third party's assent, and worse, invites the project to
  over-build against a guessed list in the interim. Keep the conversation as an
  action; do not make its outcome a gate.
- **AC-3** is already discharged elsewhere (#577 AC-1) per the issue's own
  correction comment, minus a platform residue that belongs to #284.
- **AC-4** ("one course offering runs on this fork; the course site links here")
  is a **success metric**, and #508 already states it as one at the programme
  level ("external courses running JLS labs within 18 months"). Metrics belong on
  the capstone (#517) and the review, not as a checkbox on a work item.

Replace with criteria the project can discharge alone: the fork-delta ledger
exists on master and is regenerable; the `fork-interop` CI lane is green over a
fixture set written by the other fork's writer; `docs/fork-compat.md` states the
bidirectional guarantee and `SUPPORT.md` states the term cadence; at least one PR
has been opened against bsiever/JLS. Then the outreach is what it should be — a
one-line email pointing at four artifacts — instead of the thing everything waits
on.

## Where this strengthens the arc, and one risk

It strengthens it. The project's differentiator per #508 is engineering
discipline — ratchets, goldens, reproducible builds, byte-identical outputs. The
reframing above expresses adoption *in that same currency* (a tested contract,
a CI lane, a published policy) rather than in a currency the project has no
demonstrated strength in (relationship management, single-shot persuasion). It
also converts the one asset this fork uniquely holds — a maintained, hardened,
spec'd loader that reads every `.jls` container ever written — into the actual
adoption mechanism, which is a much better story than "we are mature now."

The risk worth naming: a dialect-compatibility guarantee is a **permanent
constraint**, and this project has an appetite for permanent constraints
(#498 exclusions, recorded decisions, four ratchet tests). Freezing bidirectional
fork interop before the #77 core extraction and the #78 registry generalization
could pin the persistence layer at an awkward moment. The mitigation is in the
scope: guarantee the *dialect* (containers, tags, `-t` grammar, verdicts), not
the internals — which is precisely the line `docs/file-format.md` and
`docs/batch-interface.md` already draw, and which the grand architecture's
core/consumer split keeps drawable.
