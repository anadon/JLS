# Issue #591: FEAT-C36-4: a paper on the autograding contract and the course experience is actually submitted to a peer-reviewed venue — with the tool-paper fallback recorded in writing if the course data does not arrive
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Two different wants are fused into one deliverable, and the Outcome paragraph
says so out loud: *"JLS becomes citable ... has something with a DOI-track to
point at"* and *"an instructor who wants to justify JLS to a curriculum
committee."* Those are not the same need and they do not cost the same.

1. **Citability** — a stable identifier a bibliography can hold.
2. **Committee-grade justification** — a document a non-contributor can forward.
3. **Academic legitimacy** — peer review inside a discipline's own venue.

(2) is already filed and scoped: #589's instructor-facing white paper, whose
dedup comment on this very issue draws the boundary correctly. (3) is what this
issue actually buys. (1) — the thing the Outcome names *first* — is not filed
anywhere, is not what a SIGCSE submission delivers (a submission receipt has no
DOI; only acceptance does, and CAP-36 AC-4 explicitly disclaims acceptance), and
is nearly free. I checked: the tree has no `CITATION.cff`, no `.zenodo.json`, no
DOI anywhere in `docs/` or `README.md`. The project that publishes cosign
signatures, SLSA build-provenance attestations, a CycloneDX BOM, a `.buildinfo`
and a byte-reproducible jar has no way for anyone to cite it.

That gap is the tell. This issue aims at the most expensive of the three routes
to the goal it states first.

## Reframing 1 — make JLS citable this month, then submit on the merits

Concretely: a `CITATION.cff` at the repo root (GitHub renders a "Cite this
repository" button from it, and it is machine-readable by Zotero and BibTeX), a
Zenodo–GitHub release hook or a Software Heritage SWHID so every `v*` tag mints
an archival identifier, and the DOI badge in `README.md` next to the Scorecard
badge. Hours of work, sitting inside a release workflow that already does
strictly harder things. Do this and clause 1 of the Outcome is discharged before
any of #591's four-deep dependency chain moves — and, more importantly, the
paper stops carrying a load it was never the right tool for.

**Then** ask what paper is worth writing on its own merits, on its own schedule.

## Reframing 2 — JOSS is the venue this issue never considered

The issue names SIGCSE/WCAE/ASEE and inherits the deadline discipline of AC-1
(venue, deadline, page limits, fallback venue). But the Journal of Open Source
Software satisfies every acceptance criterion here at a fraction of the cost:
peer-reviewed, rolling submission (no deadline to slip, so AC-1's fallback-venue
apparatus evaporates), a real Crossref DOI on acceptance, a ~1000-word paper,
and — decisive for AC-4 — a review process where a named reviewer *installs and
runs the software* on a public GitHub thread. AC-3's "submission receipt" is a
JOSS issue number. AC-4's "reproducible by a reviewer without contacting the
authors" is not something JLS would have to construct; it is the review itself.

JLS is unusually well-positioned for that review: reproducible jar, BOM,
attestations, ratchets, mutation gates, an Agda proof job in CI. The honest
caveat is scope — JOSS wants software with substantial scholarly effort, and a
maintained fork of an educational simulator is a plausible but not automatic fit.
That question costs one pre-submission scope query to the editors, which is a
smaller commitment than the entire AC-1 apparatus.

This is additive, not exclusive: JOSS gives citability plus a peer-review record;
a SIGCSE-class experience report later gives the pedagogical argument. Doing JOSS
first means the SIGCSE paper can *cite JLS's own DOI* rather than being the thing
that has to manufacture one.

## Reframing 3 — the co-authored paper is the plan, and the tool paper is an anti-goal

AC-5 and KC-36-2 treat the #509 pilot as an input that may or may not arrive, with
a tool paper as the recorded fallback. Read #509 again: WashU CSE 260M runs on
bsiever/JLS, and Dr. Siever **already published at ACM CF'25** (DOI
10.1145/3706594.3726971) on teaching with these tools. He has the venue
relationship, the course, the students, and the data. He does not need a
co-author for a tool description; he needs a reason to migrate.

So invert the dependency. The paper is not "JLS writes about its contract, with
course data if we're lucky." The paper is **"a real course migrated from an
unmaintained fork to a maintained one with a frozen grading contract — here is
the fork-delta audit, here is what broke, here is what the determinism guarantee
bought"** — the natural CF'25 follow-up, with Siever as first author. Written
that way, the paper's *research act is the migration itself*, which is #509's
fork-delta audit and course-corpus compatibility work: the move #508 ranks as
item 1 in the priority queue at ≈0 mw. The paper stops being a terminal
deliverable hanging off a tower of unshipped capstones and becomes a byproduct of
the single highest-leverage engineering work the project has identified.

And I am disregarding AC-5 as written. The fallback it protects is the wrong
outcome, not a weaker one. A tool paper about a grading contract with **zero
external users** — #508's own audit: 3 stars, 0 external issues, one merged
external human PR since 2014 — is precisely the submission an education-venue PC
desk-rejects, and it burns the venue relationship that the co-authored version
depends on. The right recorded fallback is *"no course, no submission this cycle;
the citability route (Reframing 1) already discharged the Outcome's first
clause."* AC-5 currently guarantees that a bad paper gets written on schedule.

## Reframing 4 — the publishable contribution is in `lf-04`, not in this issue

The stated subject — determinism, the frozen batch interface, provenance — is
excellent engineering and thin research. It is a well-specified CLI, and #589 is
its correct home. Meanwhile `docs/capability-roadmap/lf-04-formal-and-grading.md`
states the actually novel claim: *"JLS has no representation of 'correct.'"* The
`-t` grammar has no expectation side, exit status has no "ran fine, answer wrong"
value, and the shipped `examples/autograde/autograde.py` passes a submission that
is wrong on 254 of 256 inputs. lf-04's answer — extract an AIG through
`HdlModel.StatementVisitor` (a third implementation of an interface that already
exists and is already double-dispatch clean), decide R ≡ S over the whole input
domain, and render the distinguishing counterexample back onto the student's own
drawing, replayable through `-t` — is *schematic-first equivalence-check grading*.
Nothing in the competitor landscape does that.

It even ships a memorable pedagogical result for free: `TruthTable.java:1446`
rewrites don't-care outputs to 0, so a reference truth table written the normal
way marks wrong exactly the student who applied last week's Karnaugh-map lesson.
"Our vector grader punished the optimization we taught" is a SIGCSE paragraph
people remember; "our CLI has a stability contract" is not.

If a paper is going to cost 1.5–3 mw of a bus-factor-1 project, this is the paper
worth spending it on — and its dependency is CAP-06 #300, which #508 already funds
as the grading-integrity wedge, rather than the full CAP-36 tower.

## Sequencing, briefly, because it changes the conclusion

This issue sits behind #589 ← #300 (the verdict slice, unshipped — there is no
grading verdict in the tree today) and behind #588 ← #512/#560. #588's required
citations point at `docs/reviews/evidence/2026-08-niche-survey/`, which is not on
master in this checkout — the evidence base for the related-work section is
branch-resident, exactly the risk #508/#493 flagged. Committing to an external
deadline on top of a four-deep chain of unshipped work, at a velocity baseline
#508 describes as "already tapering," is a schedule with no slack anywhere in it.
Reframings 1 and 3 both remove that coupling; Reframing 2 removes the deadline.

## What I would do

Keep the issue. Rewrite it as: (a) file citability separately and land it now;
(b) target JOSS as the peer-reviewed submission, since it satisfies AC-2/3/4
structurally; (c) make the SIGCSE-class paper a co-authored migration experience
report owned jointly with #509, filed as a sub-issue of #509 rather than of
CAP-36; (d) delete the tool-paper fallback and record "no course, no submission"
as the honest alternative. The end this issue names — JLS's claims existing as
citable, checkable evidence — is right, and strengthens the project's arc. The
route as written is the most expensive path to it, aimed at the least publishable
of JLS's three real contributions.
