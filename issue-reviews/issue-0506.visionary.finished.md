# Issue #506: CAP-25: one batch invocation over the same 300 submissions flags every planted copied pair with the matched subcircuits shown side by side — and 50 independent correct solutions all score below threshold
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Underneath the tiering, CAP-25 makes one claim: **an instructor with 300 `.jls`
files has no way to notice that two of them are the same drawing, and JLS is the
only tool positioned to give them one.** That claim is true, the gap in the field
is real, and the issue's ethical instincts are better than most of the tracker —
the null model as an acceptance criterion rather than a footnote (AC-2), the
ship-blocking framing gate (KC-25-3), the refusal to claim adversarial
completeness (§3.4). None of what follows is a complaint about care.

The problem is the route, and specifically the *object* being built: a calibrated
pairwise **similarity score** with a tuned threshold. That object is the one thing
this project has already decided, in writing, in the tree, not to build.

## The finding that has to be resolved before anything else

`docs/capability-roadmap/AMENDMENT.md:478` — under P11, the very lineage this
issue names as its substrate:

> **Deliberately refused: any similarity score, cohort ranking or automated
> plagiarism flag.** … Ship the pairwise comparison; a comparison is a tool a
> human uses after forming a suspicion, a score is a machine forming the
> suspicion.

The long form is `docs/capability-roadmap/lf-06-diff-merge-vcs.md:576-620`, which
reaches that conclusion after enumerating four failure modes, one of which
(`:610`) is precisely CAP-25's §3 risk 2. Its recommendation is explicit: ship
`jls -diff a.jls b.jls`, ship `--assert-fixed-unchanged` against the skeleton
(`fixed` already exists — `src/jls/elem/Element.java:258-271`), publish the
ethics documentation, and *do not* ship a score, a ranking, or automated flagging.
That work is already costed inside P11 as stage **C6, 2–3 maintainer-weeks**
(`lf-06:…` stage table), riding free on C2's differ and C3a's byte-deterministic
SVG side-by-side (`test/jls/SvgExportTest.java`).

CAP-25 cites lf-06/P11 four times as the canonical-form substrate and never once
mentions that the same document refuses its product. That is not a technical
oversight; it is a capstone quietly overturning a recorded determination by
building on top of it. Two independent coverage passes and a REPLAN chain ran
over this issue in the last five days and none of them caught it, because they
searched the tracker and not the tree. **Whatever else happens, this needs a
maintainer ruling on the record — either the refusal is amended with reasons, or
CAP-25 is re-aimed. It cannot be left ambiguous while #880's tasks are picked up.**

## Why the refusal is right for JLS in particular

Two structural facts, neither of which more PF work fixes:

1. **JLS's workload is the workload where scoring cannot work.** The product
   serves first-year students drawing 4-to-20-gate circuits; the issue's own risk
   2 concedes "there are only so many correct 4-gate answers", and its recorded
   escape hatch is to ship "assignment too small to fingerprint". For JLS's actual
   assignment sizes, that message *is* the shipped behaviour almost everywhere.
   The band where calibration works — designs big enough to have an idiosyncratic
   structure — is `riscv/`-scale, and nobody submits those as homework.
2. **The null model is wrong by construction if there is a skeleton.** The
   archetypal JLS lab hands out a partly-drawn file (that is what `fixed` is for).
   Fifty honest submissions derived from one skeleton are *not* independent draws;
   their canonical graphs share a large common subgraph by design. AC-2's fixture
   of "50 known-independent correct solutions" measures a population the course
   does not have, so a threshold calibrated on it will fire across the cohort on
   the first real lab.

## Reframing 1 — exact canonical equivalence classes, not a score

Delete the fuzzy middle. Canonicalize each submission (position/name/layout
erased) and **hash it**. Emit the cohort as buckets:

```
$ jls -b -classes lab3/*.jls
14 distinct canonical forms over 300 submissions
  #1  n=112   (matches skeleton + 3 gates)
  #7  n=2     alice.jls  bob.jls        ← identical after canonicalization
  ...
```

What this destroys:

- **PF-3 disappears entirely** — the 4–6 mw research core, the highest-risk band
  in the issue, the thing KC-25-1 exists to gate. There is no threshold to
  calibrate because there is no score. Bucket sizes are self-calibrating: an
  instructor reading "for this 5-gate lab, 112 submissions are canonically
  identical" needs no statistics to conclude that identity means nothing here,
  and needs none to read "n=2, and the rest of the class has 14 distinct forms".
- **PF-2 disappears too, on arithmetic.** Winnowing is justified in §2 as avoiding
  "O(n²) full-graph matching" — but the O(n²) is 44,850 comparisons of
  fixed-length fingerprints for n=300, i.e. milliseconds. The real cost is 300
  loads and 300 canonical serializations, which is seconds. AC-5's wall-time
  budget, KC-25-4's sharding fallback, and the CAP-17 duplication worry are all
  machinery for a performance problem that does not exist at classroom scale.
- **The evidence gets *better*, on the issue's own terms.** §"Intended Audience"
  wants an artifact an integrity hearing can review. "These two files have
  identical canonical text; here it is, and here is the command that regenerates
  it" is categorically stronger before a hearing than "score 0.87 against a
  threshold of 0.72 derived from fixture data". Exactness is not a weaker
  product than calibration; for this audience it is the stronger one.
- **The primitive acquires four other consumers**, which is what the issue says
  it wants for the substrate. A canonical-form hash is: the missing oracle for
  P3's "equal to the original modulo element ids" round-trip claim (lf-06 notes
  that claim has no comparator today), the fast path for the differ (equal hash →
  no diff), the dedup key #357 needs to make N placements of one subcircuit share
  an identity, and a cheap CI regression check. "Calibrated similarity score with
  a tuned threshold" has exactly one consumer, forever, and it is this issue.

What it costs: a copier who inserts one no-op buffer lands in a different bucket.
That is real. It is also the honest boundary — and it is why reframing 2 matters.

## Reframing 2 — compare deltas from the skeleton, not submissions

If a cohort-scale pass is still wanted, the object to compare is not the
submission, it is **`diff(skeleton, submission)`**. Two honest students share the
skeleton and differ in their own work; two copying students share the *delta* —
including the same wrong 3-gate structure, the same redundant inverter. This
removes the dominant term in the null distribution at its source rather than
subtracting it statistically, and it needs nothing that P11's C2 differ does not
already build. It also composes with `--assert-fixed-unchanged`, which is already
the answer to a different real cheat (rewiring the locked template).

Under this framing the cohort pass is: canonicalize → delta from skeleton →
bucket the deltas → present buckets, never pairs, never ranked by suspicion.
No verdict vocabulary is needed because nothing scores anything; the AC-3 wording
audit becomes near-vacuous rather than ship-blocking, which is a much sturdier way
to get KC-25-3's guarantee than testing templates for adjectives.

## What of the issue survives

- **PF-1 survives and is the whole of the value** — but as the erasure layer on
  #334/#356's canonical form, which is what #880's AC-2 already says.
- **PF-4 should not exist as its own feature.** C3a's SVG side-by-side is
  byte-deterministic and shipped-adjacent; Open Question 5 half-notices this and
  then defers it. A second HTML rendering path alongside C3a and CAP-24 is the
  duplication the issue elsewhere works hard to avoid.
- **PF-6's evasion suite becomes a much smaller, honest object**: a table of which
  transform classes move a submission out of its canonical bucket. That table is
  publishable as the tool's stated limits, which is exactly §3.4's posture.

## Disregarding the stated acceptance criteria

I am explicitly setting aside AC-1, AC-2 and AC-5. AC-1 and AC-2 are the "top 15
of 300 by score / 50 independents below threshold" pair — they encode the score
object I am arguing should not be built, and passing them on fixtures would
certify a discriminator whose null population does not resemble a real cohort
(no skeleton). AC-5's budget certifies a performance property that is not in
question. AC-3 and AC-4 survive intact and AC-6 survives in reduced form.

## Practical next step

Do not re-scope #880 out of existence — it is the right instinct (measure before
funding), it is correctly small, and its AC-2 already forbids the second
canonicalizer. Change what it measures: instead of "does any threshold separate
planted from independent pairs", ask **"do the 3 planted pairs land in the same
canonical bucket, and do the skeleton-deltas bucket them when raw canonicalization
does not?"** Same 30-submission corpus, same erasure layer, same 2–3 mw, no
statistics, and a result that is a fact rather than a calibration. Add a
skeleton to the corpus manifest (#880 AC-1 does not currently have one, and
without it the corpus is unrepresentative of the setting the tool is for).

**Verdict: redirect.** The need is real and JLS is the right place to meet it.
The similarity-score-with-threshold is not the shape it should take here, it
contradicts a recorded in-tree determination this issue builds on without
acknowledging, and the exact-equivalence + skeleton-delta framing reaches most of
the stated impact for a fraction of the cost, with stronger evidence and no
research risk. Resolve the AMENDMENT.md refusal on the record first.
