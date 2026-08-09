# Issue #568: FEAT-C30-2: an outsider picks a labeled first issue from a stocked shelf of fifteen, and their fork tells them green or red with no maintainer in the loop
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not fifteen labels. The end is CAP-30 AC-4: three external PRs merged in a rolling
quarter. Everything in #568 is instrumental to converting a stranger into a merged
diff, and every acceptance criterion here measures inventory rather than conversion.
The outcome paragraph is right and the project needs it. The mechanism — label 15
existing `tier:task` issues, write a restocking rule, adjust a workflow trigger —
is wrong in three specific, checkable ways.

## Finding A — the stock room AC-1 draws from is empty

The Notes say "the curation work draws candidates from existing open task-tier
issues; this feature files no new code work of its own." Against the live tracker
that premise is false. 605 open issues, 401 labeled `tier:task`, and **zero**
carrying `good first issue` today.

Read the newest one, #882 (`TASK-C367-1`, filed 2026-08-08): `band_mw: "2-3"`,
seven acceptance criteria, six re-derived evidence observations, three open
questions of which two are marked *blocks execution*, a `blocks: [682]` edge, and a
Definition-of-Done obligation to mirror a decision onto #319. That is a
multi-week design negotiation, not a PR. It is representative, not an outlier.

AC-4 excludes SimpleEditor. Apply the rest of the filter a first-timer actually
needs — no `blocked_by`, no unanswered open questions, no cross-issue mirror
obligations, no format-version bump, no ratchet-floor move, no maintainer decision
recorded mid-task — and the eligible subset of those 401 rounds to zero. The
curation work is therefore *authoring* fifteen new issues, which is the thing the
issue explicitly disclaims. Sizing the feature at 1–2 mw on the assumption that it
is a labeling pass is the single biggest error here.

## Finding B — the label is the wrong artifact; the register is

CAP-30's own diagnosis is that "the tracker's spec-prose reads as an internal
monologue." #568 opens with `feat_id: FEAT-C30-2`, `serves_capstones`, `band_mw`,
`ordering_after`. Its comment adjudicates "rule 3c" against a sibling. A stranger
who clicks `good first issue` and lands on that concludes they have wandered into
someone else's planning system. Putting a welcome mat on the monologue does not
change the monologue.

Compounding it: `.github/ISSUE_TEMPLATE/` holds `capstone.md` (9.9 KB),
`feature.md` (11 KB), and `scientific_task.md` (19 KB) — 40 KB of planning
apparatus and **no bug template**. So even a converted stranger's next step is
hostile. That is PF-1's job, which makes `ordering_after: []` wrong: this issue
strictly ordered after PF-1, or the funnel pours into a closed pipe.

## Finding C — AC-3 names the right feeling and the wrong seam

Three separate things hide inside "CI on the fork gives a green/red verdict."

1. **A trigger filter, not a permission.** `.github/workflows/ci.yml` line 9:
   `push: branches: ["master"]` (deliberate, per #47, to avoid double-building
   PR branches). A fork contributor working on `fix-thing` gets *zero* runs on
   their own fork today. One-line class of fix; nothing to do with approvals.
2. **A repository setting, not a file.** Upstream `pull_request` runs from
   first-time contributors are held by the Actions fork-PR approval setting. It
   cannot be satisfied by an in-repo change and cannot be tested by CI. Say so,
   and record the safety argument: top-level `permissions: contents: read`, no
   `pull_request_target`, no secrets in any PR-triggered job — the risk is runner
   minutes, not credential exfiltration.
3. **The verdict a fork would actually give.** Turning it on today hands a
   newcomer 14 jobs: two Windows lanes, three macOS lanes (10× minute
   multiplier), four installer-reproducibility lanes, and `gui-wayland`, which
   downloads a JetBrains Runtime. On a free account that is a quota bonfire, and
   most red X's a stranger sees will have nothing to do with their change. **A red
   verdict a newcomer cannot act on is worse than no verdict.**

The pass-1 dedup note ("no merge — same workflow files, different outcomes") is
right about scope and wrong about sequencing. #317's required-fast-lane /
long-run-lane split *is* the mechanism AC-3 needs, seen from outside the repo.
Two issues independently editing trigger semantics in a 59 KB `ci.yml` produces a
lane that is fast for maintainers and useless for strangers. AC-3 should consume
#317's fast lane, and `ordering_after` should say so.

## The reframing I would build instead

**I am disregarding AC-1's count and source pool, and AC-2 entirely.** Not because
the goal is wrong, but because a hand-curated shelf that a human must restock at a
threshold is a maintenance liability the project has already shown it can avoid.

**R1 — derive the shelf from the build; then no restocking rule can exist to
break.** JLS's whole character is machine-checked contracts: ratchet lists,
JaCoCo per-package floors, completeness sweeps. The natural, inexhaustible,
self-restocking first-task supply is already generated weekly and thrown away:
`mutation.yml` runs PIT over `jls.sim.*`, `jls.BitSetUtils`, `jls.Util`,
`jls.SpatialIndex`, `jls.collab.op.*` — 1173 mutants against an 80% floor, so on
the order of two hundred *surviving mutants*, each naming a class and a line, each
one PR, each graded by `mvn verify` with no maintainer judgment, none of them in
`jls.edit`. The report is already uploaded as an artifact. Publish it as a
`docs/first-tasks.md` (or a pinned issue refreshed by the same cron): "here is the
surviving mutant, here is the test class it belongs in, here is the command, green
means done." AC-2 evaporates — a shelf derived from the build cannot drain, cannot
go stale, and needs no rule. AC-4 holds by construction. And the work climbs the
#159 ratchet instead of running beside it.

**R2 — three walked cards beat fifteen filed ones.** Verification is the scarce
good, not inventory. Every unwalked card is a live chance to bounce a stranger, and
this repo's evidence (CAP-30: both 2026 external PR authors bounced) is that it
bounces at 2-for-2. Replace "≥15 at filing time" with: each card has been walked
end to end by someone who wrote down every place they got stuck, and the card
format is rewritten from the *first two strangers' stumbles*, not from the
maintainer's model of a stranger.

**R3 — one lane, not a fleet.** A single `contributor-check` job (ubuntu, JDK 25,
`mvn -B verify`) triggered on push to any branch of any repository, with the
existing 14 jobs held behind `github.repository == 'anadon/JLS'` or
`pull_request`. Fast, cheap, legible, and the same edit #317 wants.

**R4 — the out-of-the-box one: recruit from users, not only from Digital's
refugees.** `ghcr.io/anadon/jls -b -t tests circuit.jls` plus the documented `-t`
grammar and VCD profile means a first task can be stated entirely as *a circuit
file and an expected trace* — no Java, no Maven, no Swing, no repository
comprehension. "This circuit should do X and doesn't; here is the failing vector."
That is gradeable by CI, authorable by a TA, and it opens a recruitment pool
(students, lab instructors, autograder maintainers) that is orders of magnitude
larger than the one CAP-30 bets on and that JLS already has. CAP-30's whole
premise is that there is "exactly one reachable developer community." That is only
true if you insist the first contribution be Java.

## What survives unchanged

The outcome paragraph; AC-3's intent; and AC-4 generalized into the shelf's
governing rule — *nothing on the shelf may require a maintainer's tacit
knowledge*, SimpleEditor being merely its loudest instance.

## Measurement

Drop the counts. CAP-30 AC-4 already measures the real thing. Add one leading
indicator the count cannot fake: **started-to-merged ratio per card.** A shelf of
fifteen with zero starts is a failure that AC-1 and AC-2 both score as a pass.
