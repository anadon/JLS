# Issue #786: TASK-C591-2: a complete draft exists, reviewed by someone outside the author, with a reproducibility appendix a reviewer runs without contacting us
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Not a paper. The end state, stated plainly in #520 and #591, is that *citing JLS
is possible and recommending it is defensible* — an instructor can justify the
tool to a curriculum committee, and #509's Dr. Siever can answer "is it well
enough matured?" with something other than vibes. #786 is the drafting slice of
one chosen instrument (a SIGCSE/WCAE/ASEE-class submission) for that end. The
instrument is reasonable. The shape of the task around it is inverted in three
places, and the tracker is missing the cheapest 60% of the outcome entirely.

## The reframing I would push: artifact-first, paper as a rendering

#786 orders the work draft-first, appendix-second: AC-1 produces prose, AC-4
later produces "whatever it claims empirically." That ordering manufactures the
exact failure the issue's own outcome paragraph mocks — a paper about
determinism whose numbers were transcribed by hand from a run nobody can repeat.
Invert it:

1. **Build `docs/paper/artifact/` first**, before a word of the paper: fixtures,
   vectors, one documented command, expected output, a `manifest` recording the
   image digest and commit.
2. **Make the command digest-pinned container, not a checkout.** The repo
   already ships `ghcr.io/anadon/jls` multi-arch (amd64/arm64/riscv64), cosign-
   signed and attested (README "Container image"). `docker run --rm -v "$PWD:/work"
   ghcr.io/anadon/jls@sha256:… -b -t …` needs no JDK, no Maven, no `mvn verify`,
   and works on a reviewer's Mac. That single choice discharges "runs without
   contacting us" more convincingly than any prose, and it is the difference
   between an appendix that works in 2026 and one that works in 2031.
3. **Wire the appendix into CI as a gate**, and let the paper's tables be
   *generated* from its output rather than typed. The project already has this
   exact pattern and it is one of its best habits: `examples/autograde/autograde.py`
   is executed by `test/jls/AutogradeBridgeExampleTest.java`, and
   `docs/batch-interface.md` anchors every claim to golden tests
   (`BatchSimulationGoldenTest`, `VcdExportGoldenTest`). ARCHITECTURE.md calls
   those goldens "the oracles the normative docs cite." A paper appendix that is
   *not* an oracle is the one document in this repo allowed to rot silently.

Under this reframing the appendix has standalone value the paper does not: it is
simultaneously #509's maturity evidence, #588 AC-2's runnable appendix, and the
thing CAP-21's kits (#502) link. If the venue paper is never submitted, the
appendix still pays for itself. That is the test of a well-cut seam.

## Second reframing: the "outside reviewer" is not a formality, it is the customer

AC-2's "at least one person outside the author" is the weakest possible check and
the task treats it as ceremony. The strongest reviewer available is already named
in the tracker: Dr. Siever, who teaches CSE 260M on a JLS fork and has published
in precisely this channel (ACM CF'25, DOI 10.1145/3706594.3726971). Route the
draft review through #509 and one email produces four things at once — the AC-2
review, the AC-4 third-party appendix run, #790's "co-authorship was pursued
first" record, and hard evidence toward #509's undefined "well enough matured."
Concretely: replace "someone outside the author" with "an instructor who is not a
contributor, who runs the appendix on their own machine as part of the read, and
whose comments and dispositions are recorded." Same cost, incomparably better
signal. An anonymous colleague proofreading LaTeX satisfies the letter of AC-2
and buys nothing.

## Third reframing: the paper's novelty is provenance, not determinism

SIGCSE has seen a great many deterministic-autograder tool papers; a reviewer's
first question will be "how is this not Logisim-Evolution's `-test` plus care?"
The genuinely uncommon thing this repository has — and I do not think any
competing educational simulator has it — is the full provenance chain:
byte-reproducible jar and BOM re-verified by CI on every push, a published
`.buildinfo` pinning the exact JDK, signed build-provenance attestations, keyless
cosign on the image, and a *normative, frozen* batch-interface spec whose every
sentence carries a code anchor and a golden test. The publishable claim is not
"our grader is deterministic" but **"a grade is reproducible to the byte from an
attested build, and the chain from score to circuit to vector set to build is
checkable by a stranger"** — supply-chain reasoning applied to coursework
integrity. That framing also survives a hostile reviewer who has run into
grade-appeal disputes, which is the audience that actually cares.

This matters for #786's scope because it changes what the draft's spine is, and a
draft is expensive to re-spine after review.

## Where the task duplicates the arc

As written, the draft "covers the grading contract" (that is #589's white paper,
verbatim) and "related work citing the comparison notes" (that is #588's two
notes, verbatim). Executed literally, #786 writes the same material a third time
in a third voice, and the three copies then drift. **I am disregarding that part
of AC-1.** The criterion should be: *no new prose about the contract or the
competitors is authored here* — the paper cites #589 and #588 and contributes
only what is genuinely new (motivation, the provenance argument, the course
experience, threats to validity, the artifact description). That is not
pedantry: it turns a 0.5–1 mw drafting job into an assembly job, and it keeps
#589 the single home of the contract, which is #589's whole point.

## The sequencing problem the visionary read cannot ignore

#786's empirical core does not exist as shipped behavior. #300's own body is
blunt: today's shipped grading story is `examples/autograde/autograde.py`
comparing three literal stdout lines for one input vector, and the batch
interface has no exit status meaning "ran fine, answer wrong." I confirmed both
in tree (`autograde.py:53-57`; `docs/batch-interface.md` §1's three-status
table). #300 is a 12–20 mw capstone. So the chain in front of #786 is
#300 → #524 → #589 → #588 → #785 → #786, and a paper claiming a "grading
contract" written before #300 lands is describing a plan.

Two honest futures, and #591's fallback ladder considers neither — its only
fallback axis is "with or without course data":

- **(a) Wait for #300.** The strong paper. Realistically a year out.
- **(b) Write the paper the shipped system already supports today.** A
  reproducibility-and-provenance experience report needs *nothing* from #300:
  every claim in it is already true, already CI-gated, and already documented
  (`docs/reproducibility.md`, the repro-installers lane, the attestation
  recipes). It is submittable this cycle, it is the piece competitors cannot
  match, and it makes the later grading paper's provenance section a citation
  rather than a chapter.

I would record (b) as the near-term subject and (a) as the successor, and add
that axis to #785's note. The current fallback wording risks the worst outcome:
waiting on #300 *and* #509, then submitting something thin under deadline.

## The cheap route nobody in this tree has filed

CAP-36's outcome sentence is "citing JLS is possible." A peer-reviewed paper is
one way. There are two much cheaper ones, and I searched the tracker — neither
appears anywhere in it:

- **`CITATION.cff` + a Zenodo DOI on each release.** Absent from the repo (no
  `CITATION.cff`, no `.zenodo.json`, no mention in README). An afternoon of work
  gives every release a permanent DOI, a GitHub "Cite this repository" button,
  and a BibTeX entry an instructor can paste into a curriculum-committee memo.
  That is most of "citing JLS is possible," delivered now, independent of any
  venue deadline. It should be filed regardless of what happens to #591.
- **JOSS / JOSE (Journal of Open Source Software / Education).** Worth naming in
  #785 as first target or fallback. Its review *is* the AC-4 ritual: an assigned
  reviewer installs the software, runs it, and checks the docs, in public, on
  GitHub, against a published checklist — and it issues a real DOI. It is a
  fraction of the cost of a SIGCSE submission, its review model matches this
  project's own values (review by execution, in the open, with dispositions
  recorded), and a JOSS paper does not compete with a later SIGCSE paper about
  the course experience. #785 names only "SIGCSE / WCAE / ASEE class," which
  reads like the only shape a citation can take. It is not.

## What I would keep exactly as written

The refusal to make acceptance an acceptance criterion (#790). The insistence
that the fallback be a dated written decision rather than a quiet substitution.
The rule that no new benchmarking opens here. The #588 citation standard applied
to related work — every competitor claim to their own tracker — which is both
ethically right and, in a venue where a competitor's maintainer may be a
reviewer, tactically right. These are the parts of this tracker that make it
unusual, and they should not be softened.

## Summary of the reframing

Build the digest-pinned, CI-gated artifact first and generate the paper from it;
make the outside reviewer an instructor who runs that artifact (route via #509);
spine the paper on verifiable grading *provenance*, not determinism alone; forbid
re-authoring #589/#588 prose; add the "which shipped claim is the subject" axis
to the fallback ladder; and file the `CITATION.cff` + Zenodo DOI now, since it
delivers a large share of CAP-36's outcome without waiting on any of this.
