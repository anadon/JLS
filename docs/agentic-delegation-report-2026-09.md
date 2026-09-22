# ADR-1 Corpus Report — all 688 open issues, 2026-09-21/22

Every open `tier:capstone`, `tier:feature` and `tier:task` issue in
`anadon/JLS` was graded against [ADR-1](agentic-delegation-rubric.md) by an
independent agent that read only that issue's body, and the filled rubric
was posted as a comment on the issue. 688 issues, 688 comments, 787 agents,
~51M tokens.

**This report grades delegability, not the backlog's quality.** The two are
close to orthogonal, and conflating them would invert most of what follows.

## 1. Headline

| Band | Meaning | Count | Share |
|---|---|---:|---:|
| A | delegable end-to-end | **0** | 0.0% |
| B | delegable with one human checkpoint | 8 | 1.2% |
| C | must be specified or split first | 37 | 5.4% |
| D | human-led; agent researches, human decides | 421 | 61.2% |
| F | not agent-completable as written | 222 | 32.3% |

Not one issue in 688 can be handed to an agentic system and expected back
as a merged, debt-free change. The eight B-band issues are all
`tier:task`: **#939, #467, #410, #472, #427, #944, #940, #678**.

By tier:

| Tier | n | A | B | C | D | F | RAW mean |
|---|---:|---:|---:|---:|---:|---:|---:|
| capstone | 36 | 0 | 0 | 0 | 11 | 25 | 10.2 |
| feature | 176 | 0 | 0 | 4 | 89 | 83 | 12.1 |
| task | 476 | 0 | 8 | 33 | 321 | 114 | 16.6 |

Delegability is monotone in tier depth, as the rubric predicts: capstones
compose, so they are HL- and CF-capped by construction. The interesting
result is that it does not recover at the task tier either — 91% of tasks
are still D or F.

## 2. What actually binds

For each issue, which of the three band-setting quantities was most
restrictive:

| Binding constraint | Count | Share |
|---|---:|---:|
| DA and RAW jointly | 283 | 41.1% |
| ED alone (environment) | 164 | 23.8% |
| DA alone (open decision) | 160 | 23.3% |
| RAW alone (size/oracle/spec) | 40 | 5.8% |
| ED and RAW jointly | 38 | 5.5% |
| all three | 3 | 0.4% |

Axis pressure across the corpus:

| Condition | Count | Share |
|---|---:|---:|
| CF≤1 — whole-tree invariant or external toolchain semantics | 379 | 55.1% |
| SC≤2 — specification not closed | 363 | 52.8% |
| OS≤2 — no behavioral oracle | 358 | 52.0% |
| HL≤1 — more than one expert-week | 339 | 49.3% |
| RD≤1 — touches a published or irreversible artifact | 337 | 49.0% |
| **ED≤1 — needs physical hardware or other people** | **202** | **29.4%** |
| ED=0 — needs other people specifically | 165 | 24.0% |
| ED=1 — needs hardware or a screen recording | 37 | 5.4% |
| RD=0 — irreversible outside the repo (DOI, Maven Central, Marketplace) | 22 | 3.2% |

**Roughly one issue in four cannot be closed by any agent at any capability
level, because its acceptance evidence requires another human being.** Not
a hard task — a *categorically different* deliverable. That population is
invariant to model improvement.

## 3. Predicted debt modes

| Mode | Count | Share |
|---|---:|---:|
| `HOLLOW-ORACLE` | 332 | 48.3% |
| `FABRICATED-EVIDENCE` | 202 | 29.4% |
| `GOLDEN-LOCK-IN` | 67 | 9.7% |
| `PREMATURE-SEAM` | 59 | 8.6% |
| `SPEC-DRIFT` | 20 | 2.9% |
| `PARTIAL-INTEGRATION` | 4 | 0.6% |
| `NONE` | 4 | 0.6% |

Four issues in 688 carry no predicted debt mode.

The dominant pair is one mechanism seen from two sides. `HOLLOW-ORACLE`
and `GOLDEN-LOCK-IN` together (399, 58%) are the same configuration: **the
executor authors the artifact and the evidence that certifies it in the
same change.** The check is then a photograph of the behavior, green
forever, and — because this backlog cites its own goldens across issues —
it becomes ground truth for everything downstream.

`FABRICATED-EVIDENCE` at 29.4% tracks ED≤1 exactly. It is the most severe
mode not because it is most common but because it is invisible to CI: an
agent asked for a hardware checklist or a measurement table can produce a
plausible one, and nothing in the build disagrees.

## 4. Decomposition

185 parents have at least one graded child, covering 632 roster edges.

**Aggregate delegable fraction of all rosters: 8/632 = 1.3%.**

No capstone has a single A- or B-band child. The best features
(#593, #592, #527, #365, #357, #354, #318, #170) have exactly one each.
Decomposing a capstone into features and features into tasks has not, on
this backlog, produced delegable leaves — the tasks inherit the parent's
open decisions rather than resolving them.

Dependency structure: 369 issues (53.6%) declare at least one `blocked_by`.
Most-blocking: **#466** (blocks 18), #468 (9), #548 (9), #336 (8), #84 (7),
#524 (7), #319 (6).

## 5. Three concrete, fixable findings

1. **The docs purge orphaned deliverables.** 42 graded issues name a
   deliverable whose target file was deleted at HEAD (commit `4882e93`,
   "Clearing out irrelevant docs the LLMs have accumulated"). Their
   completion criteria now point at nothing, and graders repeatedly flagged
   that an executor would either invent a location or silently skip the
   criterion. This is mechanical to fix and currently caps otherwise-strong
   issues (#469, #466, #438, #191 among them).
2. **194 issues route something to "the maintainer."** Where that is a
   design call, it is correct and ADR-1 scores it as DA. Where it is a
   status check or a roster re-sync, it is bookkeeping that could be closed
   in the issue text and is costing a band.
3. **105 issues are specified against something that does not exist yet** —
   an unlanded seam, an unfiled task, an unwritten doc. Forward-designing
   against a future API is a legitimate planning technique and a reliable
   way to make an issue undelegable, because the executor must invent the
   contract it was supposed to consume.

## 6. Method, and one measured defect

688 independent agents, one per issue, each reading only its own issue.
No agent saw the corpus, another issue, or another agent's grade.

Internal consistency: **0/688 RAW arithmetic errors; 1/688 band
disagreement** with the recomputed band. Axis scoring is reproducible; what
follows is not an accuracy claim, only a consistency one.

**The DA axis was mis-instructed and its distribution is degenerate.** 87%
of issues scored exactly DA=1, none above 3, while every other axis showed
real spread. Cause: graders were told an unanswered structural question in
`## Open Questions & Decisions Needed` forces DA≤1, and that section is
mandatory in the JLS templates, so they read "section populated" as
"decision open."

A DA-only re-grade on a 66-issue stratified sample with a corrected
instruction measured the bias: **+1.23 mean, strictly one-sided** (41
raised, 25 unchanged, 0 lowered). Per issue, 2.42 evidence-hygiene items
were being miscounted as decisions; 33 of the 56 issues with named
decisions state a preference for **every one** of them.

Band impact, and the reason this does not change the report's conclusions:

| | observed | DA-corrected estimate |
|---|---:|---:|
| A | 0 | 0 |
| B | 8 | ~13 |
| C | 37 | ~129 |
| D | 421 | ~324 |
| F | 222 | 222 |

Every band change is D→C. **A stays 0, F is untouched**, because F is set
by ED and low RAW, not DA. The corrected reading is that ~19% of the
backlog is C-band rather than ~5% — better news for the backlog, no change
to the headline.

The posted issue comments carry the **uncorrected** DA. Read their DA row
as biased low by ~1.2, with roughly a one-in-nine chance the band should
read C rather than D.

## 7. What this does not establish

- **Predictive validity is untested.** No issue was actually delegated. The
  rubric predicts; nothing here confirms. The honest test is to hand the
  eight B-band tasks to an agent and see whether the predicted debt modes
  appear.
- **Inter-rater reliability is unmeasured** except on DA. Each issue got
  exactly one grader; the 66-issue re-grade is the only replication.
- **HL is calibrated to September 2026** and the METR series has been
  doubling every 4–7 months. HL will drift too harsh. Re-anchor HL, and
  only HL, at each doubling.
- **A wrong premise is invisible to ADR-1.** An issue can be SC=5, OS=5 and
  still be work that should not happen.
