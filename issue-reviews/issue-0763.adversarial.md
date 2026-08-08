# Issue #763: TASK-C577-2: a CI lane loads, simulates and grades every CSE 260M fixture on every change — and a circuit that grades differently than on the origin fork is a named finding, never a quiet deletion
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what was checked

Fetched #763 (open, no comments). Fetched its stated context: parent #577
(FEAT-C33-3, both comments, including the 2026-08-08 same-day "ORDERING
CORRECTION"), sibling tasks #761 (TASK-C577-1, the corpus) and #765
(TASK-C577-3, licensing/kit), and #509 (the WashU/Siever adoption issue).
Read `README.md`, `ARCHITECTURE.md`, `docs/batch-interface.md`,
`.github/workflows/ci.yml` in full, and searched the tree for `260M`,
`corpus`, `longrun`, `lane budget` — none exist in `src/`, `test/`, or
`docs/` (only in `issue-reviews/*.md`, which are prior reviews, not repo
substance). Cross-checked the CI-lane-budget precedent: #317 (FEAT-007)
and its task #378 (TASK-0016), which define the `longrun` tag / scheduled
lane / fixture-size policy #763 AC-4 invokes — both open and unimplemented
(`grep -rn timeout-minutes .github/workflows/` returns nothing; no
`longrun` tag anywhere in `pom.xml` or `test/`). Also checked #697
(TASK-C525-2, a same-shape "grade N fixtures under a wall-time budget"
precedent) for comparison.

## Findings, most severe first

### 1. AC-4 references a CI lane-budget policy that does not exist in the tree and is not in this issue's dependency list

> "The lane's cost is stated and its placement respects the repo's CI
> lane-budget policy."

There is no such policy today. `.github/workflows/ci.yml` has zero
`timeout-minutes` declarations (confirmed by grep), and no `longrun`
JUnit tag or scheduled long-run lane exists anywhere in `pom.xml` or
`test/`. The mechanism this AC presupposes is exactly what #317
(FEAT-007) and its task #378 (TASK-0016, "an hours-long test has a
scheduled lane to run in, the required gate has a stated budget") are
still planning to build — both open, both unimplemented. #763's own
machine block declares `ordering_after: ["TASK-C577-1"]` only; #317/#378
are absent. This is the same defect class already confirmed on sibling
issue #765 (its finding 1: a stale/missing `ordering_after` edge that
lets the task be picked up and started while actually blocked). Here it
is worse in one respect: #765 was blocked on a *social* dependency
(Siever's signature); #763 is blocked on *infrastructure that literally
does not exist to be respected*. As written, AC-4 can be trivially
"satisfied" by asserting compliance with a policy that has no content —
or, if #317/#378 land first with a real `longrun` tag and budget, #763's
wording gives no hint whether the new lane belongs in the required gate
or the scheduled lane, which is precisely the distinction #317 exists to
enforce.

**Recommendation:** add `317` and/or `378` to `ordering_after`, or state
explicitly that #763 defines its own interim budget number independent
of the not-yet-existent policy and will be reconciled once #317/#378
land.

### 2. Grading requires test-vector files whose provenance this issue never names, and the task that would supply them is explicitly out of scope here

> "A CI lane loads, simulates and **grades** every fixture in the CSE
> 260M corpus."

Grading in this codebase means running the `-t` test-vector grammar
(`docs/batch-interface.md` §2) against a circuit — a separate file from
the `.jls` circuit itself, containing stimulus and expected watched-value
output. #761 (TASK-C577-1, this issue's only declared dependency) commits
only "the circuits ... as compatibility fixtures with recorded
provenance" — its acceptance criteria never mention committing or
deriving grading vectors, and its own Boundary line reinforces that:
"Artifacts only." #763's own Boundary section says "Fixtures are
TASK-C577-1; the kit half is TASK-C577-3" — and TASK-C577-3 (#765) is the
one that "adapts" material, which per #765's own review is gated behind
an unresolved, non-committal licensing conversation with Dr. Siever.
Nothing in #763 explains where the grading vectors this task's core verb
depends on come from, or whether "compatibility fixtures" from #761 are
even meant to carry grading vectors alongside the raw circuits. If they
are not, "grades every fixture" is not executable from #761's stated
scope alone.

**Recommendation:** either have #761's acceptance criteria explicitly
commit per-fixture grading vectors as part of "compatibility fixtures"
(and say so), or narrow #763's AC-1 to "loads and simulates" (dropping
"grades") until the vector source is named.

### 3. "Grades differently than it did on the origin fork" has no defined oracle-capture mechanism, cost, or staleness policy

The origin fork is bsiever/JLS (per #509). Nothing in #763, #761, or
#577 states how the per-fixture "origin fork" baseline is captured: a
one-time snapshot recorded at corpus-commit time (which version/commit of
bsiever/JLS?), or something CI re-derives by actually building and
running bsiever/JLS on every push. The two have wildly different costs
and failure modes — a static baseline can silently drift stale as
bsiever/JLS itself changes (WashU's course is live and evolving, per
#509), while a live comparison means standing up a second JLS build (a
different, unmaintained fork, possibly on an incompatible JDK) inside the
same CI lane. AC-4's "the lane's cost is stated" cannot be honestly
written without first deciding which of these #763 means, and the issue
gives no signal either way.

**Recommendation:** name the oracle mechanism explicitly (e.g., "the
origin-fork's watched-output is captured once at corpus-commit time as
part of #761's provenance record, keyed to a bsiever/JLS commit hash, and
this lane diffs against that recorded baseline — never against a live
bsiever/JLS build").

### 4. AC-2's "named and dispositioned finding" is unspecified in format, and nothing stops it being used to launder every regression as accepted

> "A fixture that does not load or that diverges from its origin-fork
> behaviour is recorded as a named finding with a disposition, and the
> corpus census makes any removal visible (AC-2)."

No artifact, file, or schema is named for "a named finding with a
disposition" — it could be a one-line GitHub issue comment, a markdown
table, or a code comment, any of which technically satisfies the words.
More importantly, the AC as written treats "recorded a disposition" as
success regardless of what the disposition says: a maintainer (or an
under-scrutiny automated agent) could disposition every divergence as
"accepted, known upstream drift" without investigation, and the lane
would stay green while every fixture silently stopped matching its
origin-fork behavior. The issue's title explicitly frames the goal as
preventing "a quiet deletion," but says nothing about preventing a quiet
rubber-stamp, which achieves the same outcome (undetected divergence)
through a different door.

**Recommendation:** require the disposition to name a category from a
closed set (e.g., `KNOWN_UPSTREAM_DIFF` / `INVESTIGATING` /
`ACCEPTED_REGRESSION` with a linked follow-up issue) and forbid
"unowned"/free-text dispositions from closing the finding.

### 5. "On every change" (implying a blocking required-gate lane) is in tension with the project's own stated CI philosophy, and corpus size is unknown

The title says the lane runs "on every change" and AC-1 says it fails
"the build" on regression — both read as a required, blocking PR check.
But #317/#378 (see finding 1) exist specifically because the project has
already learned that hours-long or unbounded-size test runs do not
belong in the required gate (`docs/`/`ci.yml` header: "the required gate
stays short"). The CSE 260M corpus's size is not yet known — #761 hasn't
landed, and #763's own AC ("the corpus census... is asserted") is the
census that would establish it. A real university course ("Introduction
to Digital Logic and Computer Design," per #509) plausibly has dozens of
lab circuits including sequential/FSM designs, which are the more
expensive simulation class. If the corpus turns out to be large or slow,
#763 as scoped has already committed to "on every change," with no
architecture for demoting to a scheduled lane later without contradicting
its own title.

**Recommendation:** state the lane's target home (required gate vs.
scheduled) as conditional on the corpus census number from #761, rather
than committing to "every change" before that number is known.

### 6. Boundary gap: #763 never re-checks the redistribution-licence carve-out that #577's own ordering-correction comment attaches to AC-1/AC-2

#577's 2026-08-08 "ORDERING CORRECTION" comment (which unblocked AC-1/AC-2
to proceed ahead of #509) explicitly conditions that unblock: "If any
file's redistribution licence is not already clear from its published
source, that file waits for AC-3 like the rest of the kit; the fixture
set is allowed to be smaller than the corpus and must say which files it
omitted and why." #763 (this issue) is exactly the task that would
execute that correction's spirit for the CI lane, but neither #763's
acceptance criteria nor its Boundary section repeats or cross-references
this carve-out — AC-2's disposition machinery is scoped only to
load-failure and grading-divergence findings, not to a licence-ambiguous
fixture arriving from #761 that should never have entered the lane in
the first place. A reader of #763 alone would not know this constraint
exists.

**Recommendation:** either cross-reference #577's ordering-correction
comment directly in #763's Boundary section, or fold "licence-ambiguous
files are excluded before they reach this lane, with a stated reason" as
an explicit AC-2 sub-case.

## What's solid

- The core anti-vacuity principle — a fixture leaving the corpus must be a
  visible, named event rather than a quiet deletion — is the right
  instinct and directly addresses a real failure mode (a green CI lane
  that got there by shrinking, not by staying correct).
- Scoping discipline against #761/#765 is otherwise clean: the boundary
  line ("Fixtures are TASK-C577-1; the kit half is TASK-C577-3") correctly
  keeps this task to the CI-lane mechanism and does not re-litigate
  provenance or licensing, matching the parent #577's own boundary
  note.
- The `band_mw: 0.5-1` estimate is plausible *for the CI-plumbing work
  alone* (wiring a batch invocation over a directory), assuming the
  harder open questions above (findings 1-3) are resolved elsewhere first
  rather than inside this task's budget.

## Verdict rationale

The anti-vacuity framing and scope boundary are sound, but three of the
findings above are not nitpicks: AC-4 leans on infrastructure (#317/#378)
that is itself unbuilt and undeclared as a dependency (finding 1); the
core verb "grades" depends on test-vector files whose source is never
named and whose obvious source (#765's kit adaptation) is licence-gated
and explicitly out of this task's scope (finding 2); and the origin-fork
comparison — the issue's own titular claim to fame — has no stated
capture mechanism, so its cost cannot honestly be estimated as AC-4
requires (finding 3). These three compound: as filed, an implementer
could satisfy every checkbox in the acceptance criteria (a green lane
that loads and "grades" only the circuits that happen to have no vector
file, against an origin-fork baseline nobody defined how to capture, with
divergences dispositioned by a free-text comment) while the stated goal —
catching real regressions against a real course's material — silently
fails to hold. The issue needs another pass: name the grading-vector
source, name the origin-fork oracle mechanism and its cost, and either
pull in or explicitly defer the CI lane-budget dependency before this is
picked up.
