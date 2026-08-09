# Issue #785: TASK-C591-1: a venue, a deadline, format limits and a fallback venue are written down, so the submission is a plan rather than an intention
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this task is really for

Stripped of its logistics, this is a **commitment device**. The Outcome says it
plainly: "the paper stops being 'we should write something'." The artifact is a
note; the function is to convert an intention into something with a date attached
that someone can be held to. That function is worth having, and #591 is right
that without it the deadline is discovered rather than met.

But the task chooses the wrong facts to write down. Of the four things the note
must contain — venue, deadline, page/format limits, fallback venue — three are
*external, public, re-derivable in ten minutes, and guaranteed to go stale*.
Exactly one AC names something the project alone knows and nobody can look up:
AC-3's trigger date for the #509 fallback decision. The task spends its ceremony
on the cheap half and one bullet on the expensive half.

## The structural inversion

`ordering_after: [589]`. The white paper is 1–2 mw of writing that itself sits
behind #300's verdict slice, which is not in the tree. So the artifact whose
stated purpose is that "everything else in #591 is scheduled against that note"
is scheduled *after* the largest thing it is supposed to schedule. A calendar
that arrives after the work is a record, not a plan.

If AC-1 is load-bearing it must come first and cost nothing — which it can,
because it is a web lookup. If it is genuinely gated behind #589, then it is not
scheduling anything and its real content is the go/no-go trigger, which does not
depend on #589 at all. Either reading dissolves the ordering as written.

## The cost of success is unrecorded — and it is the biggest fact about the venue

The note is required to record page limits and format limits. It is not required
to record the one venue term that can actually stop this project: **SIGCSE and
ASEE oblige at least one author to register and present.** That is registration
plus travel — realistically four figures — payable only if the paper is accepted.

Set that against `README.md`: macOS builds are unsigned because signing "requires
paid Apple Developer Program enrollment, which this free university tool
deliberately forgoes (#128, #135)." A project that has recorded a considered
decision not to spend $99/yr is planning a submission whose success obligates
ten to twenty times that. And CAP-36 AC-4's "acceptance is explicitly not an
acceptance criterion" means the plan is structured so the cost only lands on
winning. That is a plan whose failure mode is success.

Any note worth writing states the funding path for presentation (co-author's
institutional funds via #509, an ACM SIGCSE travel grant, or "we submit only to
venues with no attendance obligation") *before* it states a page limit. I am
treating this as a missing acceptance criterion of the first order, not a detail.

## Reframe 1 — route by content, not by calendar

Deadline-driven planning is the correct instrument when capacity is reliable and
the artifact is a known quantity. Here neither holds: #508 describes velocity as
already tapering, the bus factor is one, and #591's own AC-5 concedes that *which
paper gets written* is not yet decided. Choosing a venue before knowing whether
you have a course-experience report or a tool paper is committing to a format
before knowing the content — and the two have different venues, different page
budgets, and different review bars.

Invert it. Venue slots recur annually and are plentiful; a finished draft is the
scarce thing. Set an **internal ship date for the draft** (#786) — which is a
fact about JLS's own capacity, is enforceable, and is not going stale — and treat
venue selection as a routing decision made when the draft exists, from whatever
is open. This is strictly more robust: a draft with no venue is an asset, a venue
with no draft is a missed deadline.

## Reframe 2 — the deadline-free venues make this whole task evaporate

The task inherits SIGCSE/WCAE/ASEE from #591 and never asks whether a submission
needs a deadline at all. Rolling-submission venues — JOSS above all, arXiv or
TechRxiv as a preprint step — have no CFP date, no fallback-venue apparatus, no
page limit to encode, no registration obligation, and (for JOSS) a Crossref DOI
and a public reviewer-runs-the-software thread that discharges #591's AC-4
structurally. Under that route AC-1, AC-2 and AC-4 of *this* task have nothing
left to say.

A caution on the named venues, which the note must check rather than assume:
WCAE is co-located and has not run every year, so naming it as the *fallback*
risks designating a venue with no edition open. The fallback slot is precisely
where a rolling venue belongs.

## Reframe 3 — the citable artifact this project is missing costs hours, not months

I checked the tree: no `CITATION.cff`, no `.zenodo.json`, no DOI anywhere in
`docs/` or `README.md`. A repository that publishes cosign signatures, SLSA
provenance, a CycloneDX BOM, a `.buildinfo` and a byte-reproducible jar has no
machine-readable way for anyone to cite it. If the point of the whole #591 chain
is "JLS becomes citable," the first note anyone writes should be a `CITATION.cff`
and a Zenodo release hook — landable this month, gated on nothing, and it makes
the eventual paper able to cite JLS's own identifier rather than being the thing
that must manufacture one.

## AC-4 pulls against the project's own documentation convention

"Updated when the venue's terms change, rather than left stale" creates an
unbounded upkeep obligation on a document with an audience of one, and there is
nothing in the repo that can enforce it — I found no link-checking or
freshness job anywhere under `.github/`. Contrast the tree's actual convention:
`library-survey-2026-07.md` states outright that the facts "were checked against
upstream release pages in July 2026; they are moving targets," and
`mutation-testing-trial-2026-07.md`, `flatlaf-evaluation-2026-07.md`,
`picocli-evaluation-2026-07.md` and `ISSUE-AMBIGUITIES-2026-07.md` all do the
same. JLS writes **dated snapshots that are superseded, not maintained**. It also
holds, via #589's AC-3 and `docs/batch-interface.md`'s code anchors, that a claim
should name the thing that fails when it stops being true. A perpetually-fresh
venue note satisfies neither convention: it promises maintenance the project does
not do and cannot check.

## The alternative artifact, concretely

I am disregarding this task's acceptance criteria as written. Replace them with:

1. **On #509, one dated conditional line**, owned with the pilot rather than with
   CAP-36: *"If by DATE the CSE 260M corpus loads/simulates/grades on a tagged
   release and Dr. Siever agrees to co-author, we write the migration experience
   report; otherwise no submission this cycle."* That is the trigger AC-3 wants,
   it lives where the deciding evidence lives, and it is a fact about JLS.
2. **An internal draft-ready date** on #786 — the only date this project controls.
3. **A dated venue snapshot written when the draft is ready**, in the tree's
   snapshot idiom ("checked DATE, moving target, re-verify before submitting"),
   naming submission *class* (full paper / experience report / tool demo) as well
   as page limits, and naming the presentation-funding path.
4. **Rolling venue as the fallback slot**, so the fallback has no deadline to slip.
5. **`CITATION.cff` + Zenodo, filed and landed independently**, since it delivers
   the Outcome's headline word — citable — without any of this chain.

## Why "rethink" and not "endorse-with-reframing"

The end is right and the cost is near zero either way; that argues for
endorsement. What argues against it is that every artifact this task specifies
changes: a different document, in a different place, containing different facts,
with a different owner, and with the one genuinely load-bearing fact (the
trigger) promoted from a bullet to the whole point. When the deliverable survives
none of its own acceptance criteria, the honest verdict is rethink rather than
reframing. The commitment device #591 needs is real — it just isn't a venue note.
