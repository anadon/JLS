# Issue #802: TASK-C592-1: the editor ergonomics parity catalog is published — one row per behaviour, cited to its originating complaint, graded HAVE/GAP/REFUSE
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Not a document. The catalog exists so that #596 — CAP-37's designated
forever-bucket — cannot start work nobody scored, and so PF-2..5 fund in an
order somebody can audit. Everything else in #802 is packaging around that one
function. Judge the task by whether the artifact it produces can *refuse to fund
something*, and #802 as filed cannot.

Compare #802's five acceptance criteria against its parent #592. #592 AC-3
requires a funding-score column and the KC-37-2 1.5x stop-loss expressed as a
column. #592 AC-5 requires a recorded timed baseline for the 4-bit-counter task.
Both dedup comments on #592 additionally require a per-row **owning-feature**
column (#593/#594/#595/#596/#289) so that "the long tail" is defined by
subtraction rather than by judgement at funding time. #802 carries none of the
three. What remains — cited rows, three grades, two named bsiever rows, a
blocked-on-#316 flag — is a literature review. A catalog without a score column
and an owner column does not gate #596; it decorates it. That is the single
most important thing to fix, and it is fixable by adding two columns.

## The bigger reframe: this project pins its documents with tests, and this one has none

JLS has a strong, repeatedly-applied idiom for exactly this artifact, and #802
departs from it without saying so. Every normative table in this tree is
cross-checked in both directions by a test:

- `test/jls/ExtensionPointCatalogTest.java` — "the normative table in
  `docs/extension-points.md` agrees with the constants in both directions, so
  the doc can never drift from the code" (#223).
- `test/jls/FileFormatSpecTest.java`, `SaveTagsTest` → `docs/file-format.md`.
- `test/jls/CliFlagTableTest.java` → the `JLSStart.FLAGS` table.
- `test/jls/HelpTopicsTest.java` → palette-coverage completeness; a new palette
  entry without a help topic fails the build.
- `docs/pointer-geometry-census.md` — the closest structural precedent to this
  catalog, a per-site classification census — is enforced by
  `test/jls/PointerApiRatchetTest.java`, which carries a shrinking baseline.

Against that, `docs/library-survey-2026-07.md`, `docs/standards-landscape.md`
and the 2.0 MB of `docs/capability-roadmap/` + `docs/standards-adoption/` are
unpinned prose, and nothing funds against them. #802 proposes to add the 26th
top-level document to `docs/` in the *unpinned* class and then treat it as a
funding gate. Those are the two categories the repo already distinguishes, and
the issue puts the artifact in the wrong one.

**Alternative design A — the parity catalog as an executable registry.**
Publish `docs/editor-parity-catalog.md` as a machine-readable table (id,
behaviour, source citation, grade, owning feature, score, pin, blocked-on) and
land `test/jls/ui/ParityCatalogTest.java` in the same commit, asserting:

1. every row's grade parses as exactly one of HAVE / GAP / REFUSE — #802 AC-2's
   "not scored is not an allowed grade" becomes a build failure rather than a
   reviewer's promise;
2. every HAVE row names a test class and method that **exists** and is not
   `@Disabled` (there are 34 files, 28 test classes, under `test/jls/ui/` plus
   the `test/jls/edit/` gesture suites to point at) — this is the only
   mechanical enforcement CAP-37 AC-2 will ever get, and CAP-37 has none today;
3. every GAP row names an open owning issue;
4. every REFUSE row carries a non-empty prose reason;
5. every row carries an owning feature drawn from a closed vocabulary, so #596's
   bucket is a set difference and not an argument.

That converts the catalog from a snapshot into a ratchet, costs roughly a day
inside a 0.5–1 mw band, and makes the demo slice of CAP-37 an executable claim
instead of a paragraph. It also survives the thing that will otherwise kill the
document: #316 is about to dismember `SimpleEditor` (5,852 lines today, five
recorded re-baselines in July 2026 alone). Rows keyed to today's code rot on
contact; rows keyed to a named test survive the refactor by construction.

## The grading axis is wrong, and a better one is already within reach

**I am disregarding the HAVE/GAP/REFUSE ladder as the primary measurement.** It
is an opinion scale wearing a table's clothes, which is precisely what #592's
title says the catalog exists to end. Three grades cannot order five features,
and #802 dropped the score column that was supposed to do the ordering.

**Alternative design B — measure gesture cost over a fixed task corpus.** #592
AC-5 (dropped by #802) asks for a timed 4-bit-counter build. A stopwatch reading
is unrepeatable, unattributable and dies the moment the person who held it moves
on. But once #316/TASK-0020 lands the AWT-free interaction machine, a scripted
build of the same circuit can be **replayed headlessly and counted**: mouse
gestures, drag distance, dialog fields typed, keystrokes, dialog round-trips.
"Gestures to build a 4-bit counter" is objective, is a regression test, gives
KC-37-2's 1.5x stop-loss an actual denominator, and grades GAP severity by delta
rather than by adjective. Extend the corpus to four circuits (counter, mux tree,
bussed subcircuit, state machine) and the catalog's score column writes itself.
This is the same move #316's own §5 makes for coverage: replace an assertion
with a measurement that the build re-takes.

**Alternative design C — score JLS's friction, not the incumbents' backlogs.**
The corpus CAP-37 draws from is three other projects' most-reacted issues. That
corpus is shaped by *their* user bases and *their* architectures, not by JLS's.
Logisim-Evolution #1234 ("no component search") is a symptom of a library with
hundreds of components; JLS has 35 registered types in one toolbar
(`ElementRegistry`, per #316), and PF-3 would fund a search box for a palette
you can see all of at once. Meanwhile README and ARCHITECTURE never once claim
JLS competes for switchers: the stated identity is an educational tool for a
course ecosystem whose differentiators are reproducible builds, provenance
attestation, headless batch grading, VCD interop, and normative specs — features
aimed at instructors and autograders. The genuinely JLS-grounded evidence in the
whole capstone is two course-era issues, bsiever #18 and #4. Invert the columns:
make the scoring axis *observed JLS friction* (gesture cost, the course-era
issues, the failure modes the gesture suites already encode), and demote
incumbent complaints to a hypothesis-source column. That reframing shrinks
CAP-37 honestly, kills the rows that exist only because another tool has a scale
problem JLS does not, and pulls with the project's arc instead of against it.

**Alternative design D — refusals belong in ARCHITECTURE.md, not a table cell.**
This repo already has a durable home for "we considered it and decline":
ARCHITECTURE.md's *Recorded decisions*, where i18n, the single simulation
strategy, the plugin trust boundary and the FlatLaf default each carry a
rationale **and a revisit trigger**. A REFUSE row in a `docs/` table has neither
and will be re-litigated by the next agent. Route every REFUSE to a recorded
decision with a trigger; the catalog cell then cites it. This also makes CAP-37
AC-3's "closed or refused by name" for bsiever #18/#4 mean something after the
catalog is deleted.

## Two ordering facts the issue should not be executed against as written

- **AC-4 is not derivable yet.** "Rows whose only plausible implementation is a
  `SimpleEditor` edit are flagged blocked on #316" requires knowing which
  collaborator each behaviour lands in — which is exactly the per-class census
  #316/TASK-0019 (#440) is scoped to produce and has not. Written before that,
  the flag is a guess with a hard-gate's authority. Either sequence #802 after
  #440's census, or state the flag as derived-from-#440 and leave it computed.
- **The acceptance vehicle has a dangling reference.** #592's boundary note has
  each row naming "#91 or #441 TASK-0020" as its pin; **#441 was closed as a
  duplicate on 2026-08-08**. Any catalog authored from #592's text cites a
  closed issue in a load-bearing column on day one. Point rows at #316's
  residual and at concrete test classes instead (see design A, item 2).

## Bottom line

The goal is right and well-placed in the arc: ergonomic work in a
single-maintainer project genuinely does need a gate, and putting the gate
before the fixes is the correct instinct. But #802 delivers the weakest possible
form of it — an unpinned prose document, graded on an opinion ladder, measured
against other projects' backlogs, missing the two columns that make it a gate.
Add the owning-feature and score columns, land the catalog as a table plus
`ParityCatalogTest` in one commit, replace the timed baseline with a replayable
gesture count, route REFUSEs to ARCHITECTURE.md recorded decisions, and demote
incumbent complaints to an input column. The result costs the same band, cannot
drift, cannot be quietly re-scored to unlock a favourite feature, and is the
first CAP-37 artifact that would still be true a year later.
