# Issue #520: CAP-36: JLS's superiority claims exist as published, citable evidence — reproducible head-to-head write-ups, a grading-contract white paper, and a peer-reviewed venue submission
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the four PFs away and CAP-36's purpose is one sentence: **make JLS
citable, so that adopting it is defensible to someone who is not the
adopter.** That goal is correct and it is the correct *kind* of goal for
this project. #508's success condition is "external courses running JLS
labs within 18 months"; #510's base-rate finding is that in this niche
prominence flows through papers and instructor word-of-mouth. A tool with
3 stars, zero external issues, and a genuine category-best axis
(testing/grading, 5/5, the only 5 in the whole matrix) does not have a
capability problem, it has an *evidence* problem. CAP-36 names that
correctly and prices it honestly (5–9 mw, writing-heavy) and orders it
behind #300 so the story it tells is shipped behavior first. That is good
capstone discipline and I want to endorse the end without reservation.

The instruments chosen to reach that end are the problem. Two of the four
PFs are, structurally, the same failure mode JLS diagnoses in its
competitors, and one is buried under a dependency chain it does not need.

## Reframing 1 — replace the comparison notes with an executable conformance suite

**I am explicitly disregarding AC-1 ("two comparison notes published") and
AC-2/KC-36-1 (the manual freshness gate).** Not the intent behind them —
the form.

Prose that asserts things about four other projects' defects is the most
expensive artifact per unit of durable value the project could choose:

- It is a **perpetual liability, not a deliverable.** KC-36-1 is honest
  about this and thereby proves the point: every note subscribes JLS to
  the release cadences of Logisim-Evolution, CircuitVerse, Falstad and
  Digital, forever, at bus factor 1. #588's required citation set is 11
  third-party issue numbers. The 2–3 mw estimate prices the writing and
  not the subscription.
- It is **adversarial in genre**, and the genre is where JLS is weakest.
  #510 scores JLS 2/5 on learning on-ramp, 2/5 hierarchy, 2/5 scale, 1/5
  community, and notes the first launch is an empty `JTabbedPane` with no
  discoverable example circuits. A note titled "grading determinism" that
  quotes a competitor's open bugs, published by a project an evaluator
  bounces off in ten minutes, converts a real technical edge into a
  credibility problem. #588's AC-5 (name a competitor advantage) is a
  patch over a wound the format inflicts.
- It **rots exactly the way JLS's own survey says competitor docs rot**
  ("stale Burch-era docs"; Logisim-Evolution #1546's "incomplete and
  possibly misleading" CLI documentation). Shipping documents whose
  correctness decays with someone else's commits, in a repo whose
  identity is code-anchored normative specs and ratchets, pulls against
  the project's whole aesthetic.

The elegant route is already 90% specified inside #588 and nobody noticed:
AC-2 demands "an appendix directory of committed fixtures plus a single
documented command; a reviewer running that command on a clean checkout
obtains the output the note quotes, byte-for-byte." That is not an
appendix. That is a **conformance suite** with a prose wrapper stapled on.

Concrete alternative: publish a tool-neutral **Grading & Timing
Conformance Suite** — a versioned corpus of scenarios whose expected
results are derived from stated semantics rather than from any tool's
behavior. Sequential-circuit test vectors (LE #598/#950), don't-care
inputs (#1123), NAND-latch oscillation (#2454), delay ordering versus
queue priority (CV #1412), contention (#5328). A runner with thin
per-tool adapters; JLS's adapter ships first because JLS already has the
only documented batch contract. Then:

- **We assert nothing about anyone.** We publish a suite and our own
  dated results. A third party who wants a competitor's row runs it. The
  rhetorical posture flips from accusation to invitation, which is the
  posture that actually recruits Digital's stranded contributors (#510 §5).
- **KC-36-1 stops being a gate and becomes a CI job.** Freshness is a
  re-run, not a human recheck against four trackers. The kill criterion
  is satisfied structurally instead of by vigilance.
- **It is far more citable than a comparison note.** Benchmarks and
  conformance suites are the artifact type that education/tools venues
  cite for a decade; a head-to-head write-up is cited by nobody. PF-4's
  contribution becomes "we built the suite," which is a paper; "we are
  better" is not.
- **It shares a seam with work already planned.** #560 (cross-tool
  workload harness for CAP-28) is the *performance* twin of exactly this
  harness. Building correctness/grading as prose while performance is a
  harness cuts two artifacts along different grains; one harness with two
  axes is the same money and half the surface.
- **The distribution problem is already solved.** `ghcr.io/anadon/jls` is
  multi-arch and headless by construction — "reproduces on a stranger's
  machine" is a `docker run`, today.
- **It dogfoods the product.** After #300, the suite's oracle is the same
  verdict machinery instructors use. The conformance suite becomes the
  largest graded assignment JLS runs, and every regression in the grading
  contract is caught by the evidence artifact itself.

## Reframing 2 — do not fork the white paper; re-cut the specs

PF-2/#589 asks for a document covering batch-interface stability,
determinism guarantees, and provenance, with every guarantee naming its
enforcement (AC-3). All three already exist, code-anchored:
`docs/batch-interface.md` (336 lines, explicitly "normative, and a
stability contract", with golden tests named), `docs/simulation-semantics.md`
(526 lines), `docs/reproducibility.md` (210 lines), plus README's
attestation/cosign/`.buildinfo` material.

Writing a second, instructor-voiced document alongside these creates two
sources of truth for one contract, and the marketing-voiced one always
loses the drift race. Instead: keep one source, add an audience preface
and a generated handout (a `docs/whitepapers/grading-contract` build over
the existing normative text with front matter, a limits section per AC-5,
and link checking), so the PDF an instructor forwards cannot disagree
with the spec a maintainer edits. This is a smaller job than #589 prices
and it is the only version that stays true after the next release.

## Reframing 3 — PF-3 is not capstone work, and PF-4 has the wrong author list

PF-3 (positioning statement in the README, release-announcement
discipline, 0.5–1 mw) is the highest value-per-hour item in the whole
issue, and CAP-36 has bolted it to a capstone gated on #300 and #512.
Publishing "the maintained, modern successor in the Digital tradition"
into README is an afternoon; announcement discipline is a release-process
habit belonging next to `scripts/build-installer.sh`, not a deliverable
awaiting a verdict slice. Pull PF-3 out and do it this month.

PF-4 is the deepest missed reframing. The issue treats the venue paper as
something JLS writes about itself, with #509's course pilot as an
optional strengthener and a tool-paper fallback (KC-36-2). Invert it. The
WashU/bsiever lineage **already published in this exact tool family** at
ACM CF'25 (DOI 10.1145/3706594.3726971), and per #509 Dr. Siever has been
contacted and is interested. The shortest path to a peer-reviewed citation
is not a cold SIGCSE submission — it is to become the tool of the *next*
paper in a publication lineage that already exists, co-authored with the
person who wrote the last one, with the conformance suite as the
artifact-evaluation package. That reframes PF-4 from a 1.5–3 mw writing
task with an unknown acceptance probability into a byproduct of the
adoption work #508 already ranks item 0. Keep KC-36-2's fallback; stop
treating co-authorship as the optional upside.

## One concrete blocker nobody filed

`docs/reviews/evidence/2026-08-niche-survey/` **does not exist on master**
— it is not in this checkout. #588's required citation set, and therefore
all of PF-1, is sourced entirely from a branch that #508's own process
findings flag as dying ("commit `docs/plan/**` to master before the branch
dies (#493)"). Landing that evidence corpus on master is a prerequisite of
CAP-36 that no issue in the chain names, and it costs nothing today and
everything after the branch is gone.

## Alignment with the larger arc

CAP-36 sits inside a two-quarter budget of ~30–45 mw against a filed
programme of ~1,100 mw and a demonstrated velocity of one 4-week sprint.
At 5–9 mw it would spend roughly a fifth of that budget on writing, in a
period when the shipped autograder still passes a submission wrong on
255/256 vectors (#300), the first launch is empty, and no example circuit
is discoverable. The ordering constraints show the author knew this. But
the kill criteria guard only the unlikely failure (a stale competitor
claim) and not the likely one: **publishing about capabilities an
evaluator cannot find in the product.** Add that as KC-36-3 — no note,
paper or announcement publishes while #510 §4's universal gates
(shop window, first-run, examples) are open — and the capstone's timing
becomes self-enforcing rather than aspirational.

Reframed — conformance suite instead of comparison prose, generated
handout instead of a second contract document, PF-3 freed now, PF-4
re-aimed at co-authorship, evidence corpus landed on master — CAP-36
stops being a writing project and becomes the artifact that makes JLS's
one 5/5 axis measurable by strangers. That version strengthens the arc.
The version as written spends scarce maintainer-weeks manufacturing
claims that decay.
