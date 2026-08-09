# Issue #823: TASK-C568-2: fifteen genuinely-first-issue items are labelled and each one names the files and tests it touches, so picking one requires no conversation
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The end is not fifteen labels. It is CAP-30 #514's outcome: a stranger arrives,
finds work sized to one evening, and produces a merged PR without a maintainer
in the loop. #823 is the supply side of that funnel; #822 is the feedback side
(fork CI) and #824 the durability side (restocking). Judged against that end,
the outcome sentence is right and the funnel is worth building. The *sourcing
mechanism* the issue inherits from #568 is wrong, and the *artifact* it produces
is too weak to survive a quarter. Both are fixable without changing the goal.

## The sourcing premise does not survive contact with the tracker

#568's Notes say the curation "draws candidates from existing open task-tier
issues" and "files no new code work of its own". That pool is 401 open
`tier:task` issues (605 open issues total). I sampled the oldest thirty and
skimmed the shape of the rest. They are planning-corpus specs with `band_mw`,
`ordering_after`, and multi-AC bodies: CRDT confluent merge rules (#279/#280),
registry-driven dispatch over a boot snapshot (#277), reproducible dmg (#191),
HDL hierarchical SubCircuit (#292), the SimpleEditor state-machine extraction
(#84). The narrowest in the sample — TASK-0001 #372, "every registry-keyed table
is named in one inventory and pinned by a totality test" — still requires
knowing what the extension registry is and why RegisterFile fell out of two
tables. Nothing here is a first afternoon.

Note also that no issue in the repo currently carries `good first issue`: the
count is zero, not low. This is a cold start against a corpus written for a
different reader, not a top-up.

Two consequences follow. First, AC-1's honest verification ("one-PR-sized
against the current tree — not merely small-sounding") is likely to return fewer
than fifteen passing candidates, and the quota will then pull in items that are
small only in line count. Second, AC-3's escape hatch — an item may touch
`SimpleEditor` if it "carries an explicit warning ... and why it is still
tractable" — is a tell that the author already expects the barrel to be scraped.
A mislabelled item is strictly worse than an empty shelf: the newcomer spends an
evening, discovers the context tax, and leaves with a formed judgment about the
project. The shelf's value is entirely in its truthfulness.

## Reframing 1: author the shelf from the code, do not curate it from the tracker

The genuine first-issue supply in this project is in the tree, not in the
tracker, and it appears as *families* of mechanically identical work:

- **113 hardcoded `Color.BLACK`/`Color.black` draw sites** (verified:
  `grep -rn "Color\.\(black\|BLACK\)" src` → 113). #289 wants the whole dark
  mode; a single file's colors lifted onto the `Theme` record is one PR, needs
  no editor knowledge, and is judged by a rendering/theme test.
- **33 24x24 GIF toolbar icons → SVG** (#287): `src/jls/edit/images` holds 33.
  Each icon is an independent PR with a visual acceptance signal.
- **A live duplicate**: `src/jls/images` and `src/jls/edit/images` are identical
  but for `go.GIF` (`diff -rq` confirms). Deduping them, keeping every
  `getResource` path correct, is exactly one PR and exactly one afternoon — and
  it is not in the tracker at all.
- **Named-class test gaps under the JaCoCo per-package floors**: "write the
  first test for class X" is repeatable, mechanically verifiable, and advances
  the coverage ratchet the project already runs.

(One family I checked and am *not* proposing: the `@NullMarked` ratchet is
exhausted — all 18 packages under `src/jls` already carry a `package-info.java`
with the annotation.)

Framed as families, the shelf becomes a **generator, not a list**. A generator
never drains, which dissolves #824's restocking problem structurally instead of
answering it with a scheduled nag; every item in a family shares one acceptance
recipe, which satisfies AC-4 by construction rather than fifteen times by hand;
and honesty is free, because the family's boundary *is* the context boundary.

Concretely: add `docs/first-contributions.md` (linked from CONTRIBUTING) holding
one recipe per family — the invariant, the exact command that lists the
remaining instances, the entry-point file, the test that must go green — and
file individual issues from the recipes as the shelf drains. Fifteen filed
issues then cost minutes each and are trustworthy by construction.

## Reframing 2: make the curation pass leave an artifact in the repo

I am explicitly setting aside AC-2 and AC-5 as written. AC-2 records the
file/test pointers in a *comment* on a machine-authored body ("bodies are not
rewritten in this tracker"); AC-5 records the pass as a comment on #568. Both
are tracker-only state. The expensive part of this task is the reading pass over
401 issues to decide what is genuinely small; recording its output as scattered
comments throws that judgment away, and #824's restock will pay for it again.

Put the shelf in the repository: a `docs/first-contributions.md` (or a short
`first-issues` manifest) listing the curated items with their entry points,
tests, and the reason each qualifies. Then the shelf is reviewable in a PR,
diffable, greppable, survives label churn, and gives #824's scheduled check
something to compare against besides a label count.

## Two alignment defects worth fixing while here

- **The promise contradicts CONTRIBUTING.** The title's "picking one requires no
  conversation" collides with CONTRIBUTING line 19: "Open or comment on an issue
  first for anything beyond a trivial fix." Either curated first issues carry a
  standing "claim it by commenting; no approval needed", or CONTRIBUTING exempts
  them by name. Otherwise the funnel's first instruction is a gate.
- **A green local run is not obviously reachable.** AC-4 asks for a signal a
  contributor can check locally, but `mvn verify` runs warnings-as-errors,
  SpotBugs, CodeQL's sibling checks in CI, and the JaCoCo ratchet whose per-
  package floors have already failed an unrelated PR once (#233, recorded in
  CONTRIBUTING). Curated items should be chosen so the newcomer's diff cannot
  trip a floor — prefer items that *add* tests — and the recipe should name the
  escape hatch when one does.

## Ordering, and what I would not cut

A shelf with no door is furniture in an empty room. PF-1's plain human templates
(CAP-30) and a single pinned "Start here" issue are what make fifteen labels
findable among 605 spec-prose issues whose visible surface — `band_mw`,
`ordering_after`, "AC-5: recorded on #568" — reads to an outsider as a machine's
notebook. #823 should land behind PF-1, not ahead of it.

I would not cut #823 in favour of the SimpleEditor decomposition (#84/#316),
tempting as that argument is (CAP-30 AC-5 says the capstone fails while the
5,852-line class stands, and it is the largest file by a factor of three). The
two address different failure points: the god class loses the code-inspection
duel, the empty shelf loses the person who already decided to help. At 0.5–1 mw
and no code dependency, this is cheap insurance — provided it ships fifteen true
items and not fifteen labels.
