# Issue #790: TASK-C591-3: the paper is actually submitted with the receipt recorded — and if the course pilot did not arrive, the tool-paper fallback is a dated written decision, not a quiet substitution
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

#790 is the terminal node of a three-task chain (#785 venue → #786 draft → #790
submit) under #591, under CAP-36 (#520). Strip the logistics and the claim is:
**JLS's engineering guarantees should stop being self-published assertions and
acquire an external, dated witness.** #520 states the end plainly — "citing JLS
is possible and recommending it is defensible" — and #508's base-rate finding is
that in this niche prominence flows from papers and instructor word-of-mouth.

That end is right and I endorse it. The project has already built almost
everything a credible paper would assert: `docs/batch-interface.md` is a
normative stability contract with a stated versioning rule and code anchors,
`docs/simulation-semantics.md` is a normative value/timing model,
`docs/reproducibility.md` declares a scoped reproducible artifact set with a
`.buildinfo` toolchain pin, and the golden tests
(`BatchSimulationGoldenTest`, `VcdExportGoldenTest`, `HeadlessCoreRatchetTest`)
are the oracles. A paper here is a thin wrapper over shipped work — which is the
strongest possible argument for writing it.

The route is where I disagree, on four counts.

## 1. The "fallback" is the mainline, and the issue treats it as an exception

AC-5/KC-36-2 frame the tool paper as the contingency. Read the graph: #790 orders
after #786, which orders after #785, #589 and #588; #589 orders after #300 (the
verdict slice — note `docs/batch-interface.md` contains no `verdict` today, so
the paper's headline subject is partly unshipped behavior) and #524; #591 orders
after #509. #509 itself has an open ordering correction (2026-08-08) establishing
that its AC-3 is now discharged by #577's corpus lane, and its AC-1 depends on a
conversation with an external party that has not happened. A course pilot at the
end of that chain landing inside a SIGCSE/ASEE deadline window is optimistic.

The deeper problem is that the fallback as written — *same paper minus a
section* — is the weak paper. A tool paper with no evaluation is a well-known
desk-reject shape at SIGCSE. The honest fallback is not a shorter paper, it is a
**different venue class** (JOSS, a SIGCSE demo/poster, WCAE's tool track), and
that decision belongs at venue-selection time in #785, not at the submission
deadline in #790. As written, #790 asks a maintainer facing a deadline with no
course data to record a dated note and submit anyway; the reframing gives that
maintainer a venue that was chosen for the paper they can actually write.

## 2. Citability is not the paper, and the cheap route is absent from the tracker

I searched the repo and the tracker: there is **no `CITATION.cff`, no Zenodo
integration, no DOI, no mention of JOSS anywhere**. The only `doi:` strings in
tree are citations of other people's work in `docs/hdl-support-research.md`.

So the capstone outcome "JLS becomes citable" currently has exactly one filed
route — a conference submission five dependencies deep, gated on an immovable
external date and an uncontrolled counterparty — while the durable form of
citability costs an afternoon:

- a `CITATION.cff` at the repo root (GitHub renders a "Cite this repository"
  box and emits BibTeX from it);
- Zenodo's GitHub release webhook, which mints a versioned DOI plus a concept
  DOI on every `v*` tag — the release workflow that already produces
  `SHA256SUMS`, `bom.json`, `.buildinfo` and attestations needs no change;
- the DOI recorded in README and in #589's white paper.

That is not a substitute for peer review, but it *is* the thing an instructor
citing JLS in a syllabus or a curriculum-committee memo actually needs, and it
is available now, unblocked by everything. **Concrete proposal: file it as a
sibling task under #591 (or under #520 directly) and stop letting a submission
receipt be the single point of failure for the word "citable."**

And on venue: **JOSS deserves explicit consideration in #785 and is not
mentioned anywhere.** Its review criteria are documentation quality, automated
tests, a reproducible install path, contribution guidelines, and a statement of
need — a checklist this repo over-satisfies today. It has **no deadline**
(rolling submission), which dissolves the entire scheduling risk #785 exists to
manage; its review is public and conducted in a GitHub issue, which is the same
evidentiary idiom the project already uses; and it grants an indexed DOI. The
"reviewer runs the artifact unaided" criterion (#591 AC-4, #786) is literally
JOSS's process rather than extra work. A JOSS paper and a later SIGCSE
experience report (once a pilot *has* run) are complementary, not competing —
and in that order the course data arrives when it arrives instead of governing a
deadline.

## 3. The only real work in #790 is ordered last within it

Three of the four acceptance criteria are byproducts: recording a receipt is a
byproduct of submitting; the fallback note is a byproduct of a decision that
should have been made in #785; the appendix/commit pin is a byproduct of #786.
The fourth — *"co-authorship with the #509 pilot was pursued first and the
outcome recorded"* — is the only item requiring an action nobody else performs,
and neither #785 nor #786 contains a step for it. That is a seam cut in the
wrong place: a co-author changes the venue, the framing, the empirical section
and the deadline, so the approach must precede venue selection, not trail
submission.

Stronger version of the same instinct: Dr. Siever has *already published* on
teaching digital logic with a JLS fork (ACM CF'25, 10.1145/3706594.3726971). The
highest-leverage move is not "write our paper and invite him" but "propose a
joint follow-up," where his course experience is first-party and this fork's
contract/determinism/provenance story is the technical half. **Move that
conversation into #509's criteria discussion — where a conversation with him is
already required — and let #790 record its outcome rather than initiate it.**

## 4. Pin the submitted artifact with the project's own machinery

"The appendix matches what is committed at the submission commit, and that
commit is recorded" is written as clerical work — a SHA pasted into an issue
comment. This project has better tooling for exactly this claim and doesn't use
it on itself: tag the submission (`v5.x.y` or an annotated `paper-<venue>-<date>`
tag), let the release workflow attach the jar, `bom.json`, `.buildinfo`,
`SHA256SUMS` and a signed provenance attestation, and cite *that tag* in the
paper's artifact section. Then a reviewer verifies the appendix with
`gh attestation verify` instead of trusting a comment — a paper about determinism
and provenance whose own artifact identity rests on a hand-copied SHA is the same
self-refutation #786 already names about artifacts that need an email.

## The empirical core that makes the AC-5 dilemma disappear

The best reframing: the paper's evidence should not be a course experience at
all. #577's corpus lane (per #509's ordering correction: unblocked, ready now,
no counterparty) produces something a course experience report cannot —
*"N real lab circuits authored against an independent fork load, simulate and
grade identically on our tagged releases, in a CI lane, byte-stable across
platforms."* That is a determinism and compatibility result, it is
reviewer-reproducible by construction, it is available without a pilot, and it
is exactly the claim the batch-interface stability contract makes. Build the
paper on that spine and the course-experience section becomes a welcome bonus
instead of the load-bearing member whose absence needs a dated apology.

## What I would keep unchanged

The refusal to make acceptance a criterion is correct and unusually disciplined.
The insistence that a substitution be dated and written rather than quiet is the
project's recorded-decisions habit (ARCHITECTURE.md "Recorded decisions") applied
to non-code work, and it is the right habit. The boundary excluding
post-acceptance work is clean.

## Verdict

**endorse-with-reframing.** The end — external, dated, citable evidence — is on
the project's arc and cheap relative to what is already built. Keep #790, but:
(1) file the `CITATION.cff` + Zenodo-DOI task now, unblocked, so "citable" does
not hang on a receipt; (2) push the venue-class decision, including JOSS, back
into #785 so the fallback is a venue and not an amputated paper; (3) move the
co-authorship approach into #509 and leave #790 recording its outcome; (4) pin
the submitted artifact with a tag and attestation rather than a copied SHA; and
(5) build the empirical section on #577's corpus lane so the pilot is upside, not
a dependency.
