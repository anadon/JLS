# Issue #608: TASK-C556-1: one loss-report schema — construct, disposition, location, explanation — that any importer can adopt from a written document
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#608 is `TASK-C556-1`, a task filed under the feature it belongs to (#556,
`FEAT-C29-1`), which this fleet already reviewed as `needs-rework` for
generalizing a report format from a single unbuilt example. #608 inherits
every one of that review's structural problems without fixing any of them,
and adds a genuinely circular dependency of its own in AC-4. Verified
independently against this checkout: `src/jls/imp/` does not exist (only
`src/jls/hdl/imp/`, the unrelated Yosys netlist path), `docs/` has no
construct-map or migration-report document, and none of `#323`, `#451`,
`#314`, `#556`, `#561` are closed.

## Findings, most severe first

**1. [HIGH] AC-4's own "worked example" is sequenced to not exist yet, and the machine block does not say so.**
AC-4 requires: "location is expressive enough to name a position in a
non-XML source; the Falstad text format (#561) is the worked example the
document uses to prove that." But #561's own machine block states
`ordering_after: ["FEAT-C29-1 (shared report contract — this importer
emits that shape)", ...]` — the Falstad importer is explicitly sequenced
**after** this schema, not before it. #608 wants to use Falstad as the
falsification case for its `location` design, while the issue that would
actually build a Falstad reader and hit real Falstad quirks is blocked on
#608's parent landing first. The two facts together mean AC-4 can only be
satisfied by the schema's own author inventing a hypothetical Falstad
`location` value (e.g., "line 4, token 2") rather than by an importer that
parses real Falstad text and discovers what a location actually looks
like there. That is exactly the "single-example generalization,
unfalsifiable before close" failure mode this fleet's #556 review already
named (finding 2) — #608 does not close that gap, it re-labels it as an
acceptance criterion that can be checked off with a paper example.
**Recommendation:** either drop the Falstad "worked example" language from
AC-4 and defer the AC to #561's actual landing, or explicitly scope AC-4
to "a documented, provisional, hypothetical mapping — re-verified when
#561 lands" so a reviewer doesn't mistake a thought experiment for
validation against a real source.

**2. [HIGH] The closed vocabulary in AC-1 does not obviously cover the categories TASK-0054 (#451) already committed to, and nothing in #608 checks the fit.**
AC-1's closed vocabulary is `mapped / mapped-with-caveat / refused /
dropped-by-design`. But #451 (TASK-0054, the actual `.circ` reader this
schema generalizes) already specifies its own four-outcome realization map
in §7.10: `mapped(e), approximated(e,r), unmapped(r), refused(r)`. `mapped`
and `refused` line up; `mapped-with-caveat` plausibly maps to
`approximated`. But `dropped-by-design` and `unmapped` are not obviously
the same thing: #451's `unmapped(r)` is a per-construct gap that could
still be closed by a future mapping, while "dropped-by-design" reads as a
*permanent* scope exclusion (the sense #561 AC-1 uses it in, for Falstad's
analog elements — "named losses by design"). Collapsing "not yet mapped"
and "permanently out of scope" into one bucket defeats AC-1's own stated
purpose — "so a grading script can branch on it" — because a script
cannot tell "revisit this later" from "never will support this" from the
same disposition value. Re-expressing #451's actual report through this
vocabulary is explicitly deferred to TASK-C556-2 (see finding 3), so this
mismatch will not surface until after #608 has already closed and the
schema is presumably frozen as a contract other importers are adopting.
**Recommendation:** either add a fifth disposition or split
`dropped-by-design` from a genuine "not yet mapped" value before close, or
require #608's own Definition of Done to include re-expressing #451's four
categories (even from its issue text, since the code doesn't exist yet)
against the new vocabulary and flag any lossy collapse.

**3. [MEDIUM] The totality-assertion infrastructure that #556's own AC-2 requires has no home in #608, and its sibling task has no issue number.**
#556 (this task's parent feature) states AC-2 as: "The report-totality
assertion ... is provided by the shared infrastructure once, not
re-implemented per format." #608's boundary notes assign that exact work
elsewhere: "Re-expressing the existing `.circ` report through this schema
and the totality assertion are TASK-C556-2." Unlike TASK-C556-1 (this
issue, #608), TASK-C556-2 is named only by task ID — no issue number is
given anywhere in #608's body, and `mcp__github__issue_read` on #608
itself returns no comments and no linked sub-issues (`has_children:
false`). #556's own adversarial review already flagged (finding 4) that no
concrete enforcement mechanism is named for "not re-implemented per
format" — #608 does not name one either, and defers the entire totality
checker to a task that does not yet exist as a trackable issue.
**Recommendation:** either fold a minimal totality-checker stub/interface
into #608's own scope (even if `.circ` round-tripping through it is
TASK-C556-2's job), or file TASK-C556-2 now and add a real `blocked_by`
edge from #608 mirroring it, so the schema and its enforcement mechanism
don't drift apart across two issues, one of which is currently untracked.

**4. [MEDIUM] The cost band leaves implausibly little room for the sibling task, and #608 is silent about it.**
#608 declares `band_mw: "1"`. Its parent, #556, declares the *whole*
feature (both TASK-C556-1 and TASK-C556-2) at `band_mw: "1-2"`. If #608
alone consumes the full low end of the two-task feature budget, the
remaining budget for TASK-C556-2 — re-expressing #451's real report
through the new schema, building the totality assertion (finding 3), and
discovering any taxonomy mismatch (finding 2) — is 0 to 1 week. #556's own
review already called this whole feature's 1-2 mw band optimistic
(finding 5) for reasons that land squarely on TASK-C556-2's plate. #608
carries none of that caveat forward into its own numbers.
**Recommendation:** carry an explicit note in #608 that its 1 mw figure
assumes TASK-C556-2 is comparably cheap, and flag for re-costing if
TASK-C556-2's round-trip work surfaces a taxonomy mismatch (finding 2) or
a totality-checker design (finding 3) that doesn't fit in the residual
budget.

**5. [MEDIUM] No machine-enforced ordering edge exists — `ordering_after` is prose only, exactly as #556's own review already found and #608 repeats verbatim.**
#608's machine block is `task_id / part_of_feature / band_mw /
ordering_after: [323, 314]` — no `blocked_by` field at all, unlike #323
and #314 themselves, which carry mirrored `blocked_by`/`blocks` arrays per
the repo's own "Link pass" convention (visible in both issues' bodies).
#556's adversarial review flagged this identical gap for the parent
feature (finding 1: "nothing in the issue's machine block (`blocked_by`)
enforces that ordering"). #608, filed after that review existed, does not
correct it for the task level either. A contributor who opens #608 and
checks only its own machine block has no automated signal that #323
(still open, with `planned_tasks` still containing "not filed"/"planned"
language as of this reading) or #451 (TASK-0054, still open, itself
blocked on #404) have not landed.
**Recommendation:** add `blocked_by: [323, 314]` (or more precisely `[451,
314]`, since #451 is the concrete task producing the report this schema
generalizes — #323 is the feature wrapper) with the mirror written on the
far issues, per the convention #323/#314 already demonstrate in this same
repository.

**6. [LOW] "so a grading script can branch on it" (AC-1) has no consumer or test named.**
The stated purpose of the closed vocabulary is that "a grading script can
branch on it," but no acceptance criterion requires a script, test, or
even a documented example consumer to exercise that branching. As written,
AC-1 is satisfiable by an enum with no verified external consumer — the
grading-script claim is unfalsifiable within #608's own scope.
**Recommendation:** add a minimal acceptance check — e.g., a fixture test
that reads the machine-readable rendering and asserts a program can
branch on `disposition` without parsing free text — or drop the
grading-script framing from the rationale if it is not actually being
tested.

## What's solid

- The boundary discipline ("must not fork, re-implement or alter `.circ`
  mapping decisions — only the report's shape is in scope") is precise and
  consistent with #556's own boundary against #323, and correctly assigns
  `.circ` semantics ownership elsewhere.
- AC-2's requirement that machine- and human-readable renderings be
  golden-tested from one in-memory report so they "cannot drift" is a
  sound, checkable design constraint, and correctly cites its parent
  criterion (CAP-29 AC-3) rather than inventing a new one.
- The sibling-merge rule ("if #311/#323's REPLAN prefers absorbing this
  generalization, it merges there, lower number winning") is carried down
  correctly from #556 and gives a clean resolution path if the split turns
  out to be premature.
- Choosing a closed vocabulary over free text for `disposition` (AC-1) is
  the right instinct in principle — the problem is coverage (finding 2),
  not the choice of a closed set.

## Recommendation

Before this issue is picked up for execution: (a) rewrite AC-4 so the
Falstad "worked example" is explicitly a provisional, unvalidated mapping
pending #561, not proof; (b) reconcile AC-1's four-way vocabulary against
#451's own four-outcome realization map and resolve the
`dropped-by-design` vs. `unmapped` ambiguity before the schema is treated
as frozen; (c) either bring a minimal totality-checker obligation into
#608's own scope or file TASK-C556-2 now with a real `blocked_by` mirror;
(d) add machine-enforced `blocked_by` edges instead of prose-only
`ordering_after`, naming #451 specifically since it is the actual
in-flight deliverable this schema generalizes from.
