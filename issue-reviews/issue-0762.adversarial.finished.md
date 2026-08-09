# Issue #762: TASK-C545-2: an honest feature comparison against the four incumbents, and every badge that answers no real question is removed
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of the issue

Sub-task of FEAT-C27-1 (#545), itself part of capstone CAP-27 (#511). Wants a
README comparison table against Logisim-Evolution, Digital, CircuitVerse and
Falstad, plus badge-strip curation, with content shared (not copied) into
#553's switcher pages. `ordering_after: [TASK-C545-1]` (#760, open — README
screenshots/GIF/drift-check task) is a real, verified dependency.

## Findings, most severe first

**1. The cited acceptance criterion doesn't exist where cited.** AC-1 reads:
"every JLS claim traceable to `docs/hdl-support-research.md` or to a
reproducible in-tree fact (CAP-27 AC-2)." I fetched CAP-27 (#511) directly:
its actual AC-2 is "A scripted fresh-user protocol (documented, re-runnable)
measures install→running-example in <10 minutes on Windows, macOS, Linux" —
a first-run-timing criterion, unrelated to claim traceability. The
traceability/honesty language #762 is quoting instead matches **FEAT-C27-1
(#545) AC-2** almost verbatim: "honest per `docs/hdl-support-research.md` —
no claim a fresh clone cannot reproduce (#73 §4 invariant)." This is a wrong
citation, not a paraphrase drift. A reviewer who actually opens CAP-27 looking
for "AC-2" to check compliance against will find a criterion about install
timing and either reject the PR for not addressing it, or wave the real
requirement through unchecked. **Fix the citation to `#545 AC-2` before work
starts.**

**2. AC-1's source list can't actually produce the losing row AC-2 demands.**
AC-1 permits only two sources: `docs/hdl-support-research.md` (which I read
in full — it is scoped entirely to HDL export/import/co-simulation
feasibility, licensing, and staging; it contains no rows on install
friction, community size, hierarchy/reuse, or scale/perf) or "a reproducible
in-tree fact" (i.e., something checkable from JLS's own source — which by
definition cannot state a *comparative* fact about a rival tool). Yet the
dimensions where #510's teardown shows JLS genuinely losing are exactly the
ones neither source covers: community size **1/5**, scale/perf **2/5**,
hierarchy/reuse **2/5**, extensibility **2/5**, on-ramp:learning **2/5**
(all from #510 §1's score matrix). The Outcome paragraph does say "honest
per `docs/hdl-support-research.md` **and the #510 teardowns**," but AC-1's
literal, checkable text drops #510 as an allowed source. That gap is
gameable exactly the way AC-2 tries to prevent: an implementer can satisfy
AC-1 + AC-2 to the letter by adding one soft, HDL-flavored losing row (e.g.
"VHDL export: not yet, unlike Digital") sourced from
`hdl-support-research.md`, while omitting the far more damaging #510
findings (1/5 community size, no chronogram, no benchmark) — passing review
on the letter while the "honest, including where JLS loses" spirit fails.
**Recommendation:** explicitly list #510's teardowns as a required source in
AC-1, and have AC-2 require the losing row(s) to cover at least the
dimensions #510 scored ≤2/5, not merely "at least one."

**3. Badge-curation ACs presuppose a badge strip that doesn't exist.**
README.md currently has exactly **one** badge (OpenSSF Scorecard,
`README.md:3`). The Outcome text ("its badge strip is curated down") and
AC-3 ("decorative badges are removed") describe pruning a multi-badge strip
that isn't there — `docs/standards-adoption/02-openssf-badge.md:46` itself
confirms the Scorecard badge is currently the only one in the evidentiary
tier being discussed, and the only candidate addition (an OpenSSF Best
Practices badge, tracked separately under #287) hasn't landed. As written,
this quarter of the task is satisfied by one sentence justifying the single
existing badge — a near no-op dressed as a deliverable, and a sign this
issue's text was templated from #545/#511 language without checking the
current tree. Not fatal, but worth correcting the framing so the task isn't
scoped around a badge-bloat problem that doesn't currently exist, and so a
reviewer doesn't go looking for badges to cut that aren't there.

**4. Single-source-of-truth goal has no enforced dependency edge.** The
Outcome's whole rationale is "written once here and referenced from #553's
switcher pages rather than maintained twice." But #553's own metadata sets
`ordering_after: []` ("startable now — importer links are appended when
CAP-16/CAP-29 land, they do not gate the pages") — nothing in either issue's
structured metadata makes #553 wait on #762/#545. If #553 is picked up by a
different work-stream in parallel, it can draft its own comparison prose
before #762's canonical content exists, recreating precisely the
"maintained twice" duplication this issue exists to prevent. The
coordination currently lives only in issue-body prose on both sides, not in
an ordering constraint. **Recommendation:** add `762` (or `545`) to #553's
`ordering_after`, or otherwise make the dependency mechanically enforced
rather than advisory.

**5. "Justified in one line in the PR" is unenforceable by CI.** AC-3's
badge-justification requirement is a PR-description convention with no
automated check — contrast sibling #545's AC-4, which specifies a concrete
`ReadmeOnboardingTest`-style drift check for image paths. Low severity given
there is currently one badge to justify (see #3), but if the OpenSSF Best
Practices badge (#287) lands before this closes, "one line per badge" has
no gate keeping a future PR honest.

## What's solid

- The `ordering_after: [TASK-C545-1]` dependency is real and verified: #760
  is open, is about README screenshots/GIF/drift-check, and plausibly needs
  to land first so the comparison table and badge edits don't collide with
  the layout #760 establishes.
- The four named competitors (Logisim-Evolution, Digital, CircuitVerse,
  Falstad) exactly match #510's teardown set — the comparison targets are
  well-chosen and already have sourced material behind them.
- "Write the comparison once, reference it from #553 and the gallery rather
  than duplicating" is the right design instinct even though (see #4) it
  isn't yet mechanically enforced.
- AC-2's "no losing rows = dishonest, fails review" instinct is the correct
  guardrail in principle; it's the sourcing gap in #2 above that undercuts
  it in practice.
