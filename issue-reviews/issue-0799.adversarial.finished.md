# Issue #799: TASK-C587-1: every documented flag resolves to a FLAGS entry and every FLAGS entry is documented — the doc side of the triangle CliFlagTableTest never reads
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

Extend `test/jls/CliFlagTableTest.java` (which already ties `FLAGS` to
`usage()` and to the parser) with a third leg: cross-check `FLAGS`
against flags named in `docs/batch-interface.md`, the README, and the
help content tree, in both directions.

## Findings, most severe first

### 1. [Critical — contradicts a recorded project decision, task will fail CI at HEAD] AC#2 is already false against the current tree, for reasons the project has explicitly ratified

The issue's AC#2 requires: "every `FLAGS` entry is documented in at
least one named source [docs/batch-interface.md, README, help content
tree]." That is false today for two live flags:

- `board` and `pins` (`src/jls/JLSStart.java:782-786`, both
  `Arity.REQUIRED`) do not appear anywhere in `README.md` (verified:
  `grep -n '\-board\|\-pins' README.md` → no hits), nowhere in
  `docs/batch-interface.md` (same grep → no hits), and nowhere under
  `resources/help/**` (`grep -rl '\-board\b\|\-pins\b' resources/help/`
  → no hits). They are documented only in
  `docs/icestick-bitstream-handoff.md`,
  `docs/hdl-support-research.md`, and files under
  `docs/standards-adoption/`, none of which are in the issue's named
  source list.

- This is not an oversight this task happens to be catching — it is a
  decision the project already litigated and wrote down. Quoting
  `docs/standards-adoption/OPEN-QUESTIONS.md:207`: *"BATCH-INTERFACE §1
  SCOPE — sections 01 and 11 treat any new CLI flag as an edit to
  `docs/batch-interface.md` §1; section 08 (IP-XACT) correctly reads §1
  as covering only the `-b` batch synopsis and deferring to
  JLSStart.FLAGS. Verified: §1's synopsis line omits `-i`, `-export`,
  `-board`, `-pins`, `-savetext` entirely… Corrected 01 and 11 to match
  08's reading."* The maintainers concluded, in writing, that
  `docs/batch-interface.md` is deliberately scoped to the batch
  synopsis and does not need to carry every flag.

- Consequence: implementing AC#1+AC#2 literally as written makes the
  new assertion red on day one, against unmodified `main`, for
  flags nobody planted as a violation. The issue frames the outcome as
  "an undocumented new flag becomes a red build" (i.e., a ratchet
  against future drift), but the acceptance criteria as stated ratchet
  against the *present* state too, and the present state is
  intentional per the quoted decision. Either the issue needs to widen
  its named-source list (which reopens the exact scope question
  `OPEN-QUESTIONS.md` already closed) or exclude `-board`/`-pins`/etc.
  by design (which the issue never says, and which an implementer has
  no way to discover without reading `docs/standards-adoption/`, a
  ~600-line internal research tree the issue doesn't cite).

**Recommendation:** Before this task is picked up, the issue must either
(a) name the actual, current documentation home for every `FLAGS` entry
(audit first, write test second), or (b) explicitly carve out
`-board`/`-pins` (and confirm `-export`/`-savetext`, which *are* in
README, are not similarly exposed) with a rationale that doesn't
re-litigate the closed OPEN-QUESTIONS.md question. As written, a
competent implementer either ships a test that's red at merge time, or
quietly expands "documented" to include roadmap docs the project has
gone out of its way to keep informative-only.

### 2. [High — gameable acceptance criteria] "Help content tree" flag-extraction is unspecified and easy to satisfy without catching real drift

`resources/help/execution/execution.html` documents CLI flags in prose
with no consistent lexical convention — flags there are concatenated
with their operand names with no separator or backticking (e.g.
`-dtime`, `-sname`, `-sparameters`, `-tinputsigs`, `-pprinter`), unlike
`FLAGS`' `flag`/`operandName` split or the backtick-quoted `-t file`
style in `docs/batch-interface.md` and README. A regex written against
today's three sources (e.g. "match `` `-x` `` in backticks, or
`-x` at start of a help-page line") will happily pass CI while missing
real cases: a flag mentioned in ordinary sentence prose without
backticks, or a genuinely undocumented flag whose name happens to be a
prefix of a documented one (`-v` vs. `-vcd`, the exact ambiguity
`JLSStart` already special-cases at `JLSStart.java:756`). The issue's
AC#3 only requires demonstrating *two* planted failures (one
undocumented flag, one documented-but-nonexistent flag) — passing those
two specific plants proves nothing about extraction robustness against
the HTML-prose format actually used in `execution.html`. Nothing in the
acceptance criteria forces the extractor to handle that format, so an
implementer can legally satisfy AC#1-3 with an extractor that only
understands the README/batch-interface.md backtick convention and
silently no-ops on the help tree.

**Recommendation:** Either pin the required extraction formats
explicitly (cite `execution.html`'s conventions by name) or require a
fourth planted-failure case specifically inside a help HTML page using
its native prose format, so the test can't be satisfied by an extractor
that only reads Markdown-style docs.

### 3. [Medium — hidden/unverifiable dependency] `ordering_after: [TASK-C584-2]` names a task this review has no way to check

The issue is scaffolded with `task_id: TASK-C587-1`,
`part_of_feature: 587`, `band_mw: 0.5-1`, `ordering_after:
[TASK-C584-2]` — none of which correspond to a real GitHub issue
number in this repo (no open/closed issue titled or numbered as
TASK-C584-2 was found; the tracker uses plain numeric issue IDs
elsewhere, e.g. #71/#72 cited by `CliFlagTableTest`'s own doc comment).
The issue has zero comments, so there is no discussion thread
clarifying what TASK-C584-2 delivers or why this task is ordered after
it. If TASK-C584-2 touches `CliFlagTableTest.java`, `FLAGS`, or the doc
files this task reads, the two tasks could conflict or duplicate work,
and nothing in issue #799 says what to check before starting.
`band_mw` (a numeric range, "0.5-1") is used with no definition
anywhere in the repository's docs — its unit and meaning are opaque to
a reader of this issue alone.

**Recommendation:** Either link `ordering_after` to a resolvable
artifact (PR, commit, or a real issue number) or drop the field; define
`band_mw` once, in a place this issue can point to.

### 4. [Medium — underspecified deliverable] "transcripts recorded" (AC#3) names no location or format

"Both failure directions are demonstrated… with transcripts recorded"
does not say where: a PR description, a checked-in fixture file, a
code comment, CI logs retained how long? Every other task-style issue
in this repo that this reviewer sampled (e.g. the golden-test issues
citing `docs/batch-interface.md`) at least anchors artifacts to a file
path. Without that, "transcripts recorded" is satisfiable by a
throwaway paste in the PR body that vanishes from repo history,
defeating the apparent intent (a durable, checkable record that the
red/green transition actually happened).

**Recommendation:** Specify a concrete artifact, e.g. a short
`docs/` or test-comment note describing the two planted failures and
their exact assertion messages, analogous to how `CliFlagTableTest`
itself documents issue #71/#72 inline.

### 5. [Low — minor internal inconsistency] AC#4's "names the file and the contradicted source of truth" is not obviously satisfiable for the reverse direction without extra plumbing

For "documented flag not in FLAGS" (forward direction), naming the
file is easy — the extractor already knows which doc it read the flag
from. For "FLAGS entry undocumented anywhere" (AC#2's direction), there
is no single file to name — the failure is an absence across three
sources. AC#4 demands "the file" (singular) in the message; the
reverse-direction assertion can at best name all three source files it
checked and came up empty, which is a different shape of message. Not
fatal, but the acceptance criterion's phrasing doesn't anticipate its
own asymmetry.

**Recommendation:** Reword AC#4 to allow "the file(s) checked" for the
reverse direction, or accept that the two directions will need visibly
different message templates.

## What's solid

- Framing the work as a third side of a "triangle" that reuses
  `CliFlagTableTest`'s existing FLAGS/usage/parser bindings is a sound,
  minimal-surface design — it doesn't ask for a new mechanism, just a
  new comparison.
- AC#5 ("CliFlagTableTest keeps passing unmodified") is a good,
  checkable non-regression guard and costs nothing to verify.
- The bidirectional requirement (AC#1 and AC#2 both directions) is the
  right shape for a drift test in principle — the flaw is in which
  sources it's checked against today, not the direction of the check.

## Verdict rationale

`needs-rework`: the core idea is sound and cheap, but AC#2 as written
is falsifiable against the current, intentionally-scoped state of
`docs/batch-interface.md` (finding 1), which means either the issue
must be re-scoped with an accurate source list before implementation,
or an implementer will ship a test that's red at merge — or will
quietly launder scope creep into which docs count as "the" source of
truth, undoing a decision the project already made in
`docs/standards-adoption/OPEN-QUESTIONS.md:207`.
