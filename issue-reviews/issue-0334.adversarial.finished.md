# Issue #334: FEAT-003: a saved circuit is a reviewable text artifact whose diff is proportional to the edit, not to the file
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of what I checked

Issue is open, two comments (both the maintainer's own "STAGE 3" boundary-review
passes, not third-party review). I read README.md, ARCHITECTURE.md, and the
cited source: `src/jls/Circuit.java` (saveContainer field, save() ordering
comparator, stateHash()), `src/jls/elem/WireEnd.java` (ref emission),
`src/jls/elem/Element.java` (stableId/id fields), `src/jls/elem/ElementId.java`
(compareTo/legacy replica), `src/jls/elem/SaveTags.java` (WRITABLE table),
`src/jls/FileAbstractor.java` (MAX_CIRCUIT_TEXT_BYTES), and `docs/file-format.md`
(grammar). I also pulled the two child tasks (#436, #437) and three of the
graph neighbors (#315, #319, #356) and one issue #334 cites as precedent
(#488) to check whether the dependency graph and evidentiary claims hold up.

## Findings, most severe first

**1. (High) The evidentiary apparatus is anchored to a commit that does not exist in this repository, and the issue's own later comment admits it.**
`evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` is cited roughly
20 times as the pin for every code quote, every `git grep` transcript, and
every "verified ABSENT at 2d0ca9d" claim. `git cat-file -t 2d0ca9d` in this
checkout returns `fatal: Not a valid object name`. The `blocked_by: [315]`
rationale further leans on "Commit 970db41 is the precedent: two registered
element types were missing from the frozen tag table" — that object also
does not resolve (`git cat-file -t 970db41` fails). The issue's own
2026-08-08 comment concedes this: *"this issue cites a commit that does not
exist... that object is not in the repository"* and redirects readers to
#488 instead. A reviewer who tries to verify any single quoted line/output
pair against real git history cannot do so from the citation given; they
have to know to distrust the citation and go find the substitute evidence
themselves. (My own spot-check of the *content* of the citations — line
numbers in `Circuit.java`, `WireEnd.java`, `Element.java`, `SaveTags.java`,
`FileAbstractor.java` — happens to still match current HEAD, so the
technical claims are not false, but that is luck/coincidence, not something
the issue's own verification discipline (rule 6, "re-verify... at the
executor's checkout") can rely on, since the commit to diff against is gone.)
**Recommendation:** before work starts, re-pin every evidence citation to a
resolvable commit on `origin/master` (as #488 already did, citing `8288226`
instead), and strike or replace the `970db41` precedent citation.

**2. (High) The roster table and machine block are stale against the tracker, flagged twice, still uncorrected in the body.** §2's roster table reads
"TASK-0005 (planned) | ... | not filed" and "TASK-0006 (planned) | ... | not
filed", and the mermaid graph labels both `(planned)`. Both are in fact filed
and open: #436 (TASK-0005, created 2026-08-03, `part_of_feature: 334`) and
#437 (TASK-0006, same). Two separate maintainer review passes on this exact
issue — the 2026-08-04 comment ("Defect... roster is stale") and the
2026-08-08 comment ("Defect — roster is stale against the tracker... No body
edit was made") — both name this defect, and neither resulted in a body
edit. The issue's own Definition of Done requires "Machine block, roster
table, and mermaid graph agree with reality at close" — by the issue's own
repeated admission this is *already* false and has been left false across
two review cycles. This is a real process risk: if the self-correction
protocol ("REPLAN:" comments) reliably fails to close the loop on something
this issue itself flags twice, there is no reason to trust it will close the
loop on the substantive claims (dependency edges, cost bands, sort-key
decisions) that are harder to check than "is #436 filed."
**Recommendation:** edit the body (not just add a third comment) before
anyone picks up TASK-0005/0006 work, so a contributor reading the issue
top-to-bottom does not act on "not filed."

**3. (Medium) The feature is blocked on an open, 0%-complete prerequisite whose own cost estimate is admittedly unreliable.** `blocked_by: [315]`. #315 is
open, has one sub-issue, 0% complete, and its own Open Question 1 states its
task-row sum (2.1 wk) already exceeds its own cost band (1-2 mw) with the
gap explicitly unreconciled. #334's stated cost ("Band: 2-4 maintainer-weeks... Sum of this feature's own task rows: 3 wk") is scoped to #334's *own* two
tasks only and silently excludes the #315 prerequisite's cost and schedule
risk, even though #334 cannot start substantive work until #315 lands (per
its own critical-path statement in §6, and per the `blocked_by` edge which
Global Invariant 4 also leans on: "no new suppression in the
registry-totality checks #315 owns"). A reader pricing "how long until FEAT-003 ships" from this issue alone will underestimate it.
**Recommendation:** the cost section should either say "3 wk once #315
lands" explicitly (it currently reads as if 3 wk is the whole answer) or
note the #315 float as a dependency risk the way #436/#437 do for other
things.

**4. (Medium) The headline acceptance numbers ("at most 12 lines in at most 2 hunks") are stated as settled in #334 but are, by its own child task's admission, not yet independently derived.** #334 §1 item 3 and §5 IC-1 both assert the bound as fact. #436 (TASK-0005), which #334 requires to actually
land the mechanism, lists as **Open Question 3, blocking**: *"What is the
stated constant C, and on which fixture? It must be fixed before the test is
written, not fitted afterwards. Recommended default: FEAT-003's own
acceptance number."* That is circular — #334 presents 12/2 as if it were
independently derived, while #436 says the number is only a *recommendation*
carried forward from #334, not yet fixed by measurement. Nothing in either
issue shows the arithmetic behind "12" (e.g., how many `id`/`sref
attach`/`sref wire`/`probe` lines a single insertion at a wired,
interleaved-replica fixture is expected to touch). #436 separately flags
"diff-tool dependence" (context lines, algorithm) as a threat to validity
for the same number. As written, an implementer can satisfy the ratchet by
picking whatever bound the actual implementation produces and writing that
literal into the test — which is exactly the kind of acceptance criterion
the completion checklist elsewhere warns against ("the bound is judged by
the *ratio* being 1, not merely by the counts being small" — a good
instinct that the *absolute* constant, unlike the ratio, has no independent
derivation).
**Recommendation:** derive C from the actual field/line inventory of a wired
sref block before either issue treats 12/2 as fixed, or drop the specific
number from #334 and let #436 own it entirely.

**5. (Medium) Acceptance criterion §1.3's "third replica-id class" is gated on two contingencies neither issue commits to resolving.** The criterion
requires the diff ratchet to hold "for every replica-id class — a 32-hex
draw, a string sorting before `legacy`, and a string sorting after it."
Verified in code: `ElementId.compareTo` (`src/jls/elem/ElementId.java:278-285`) orders by replica string first, and `LEGACY_REPLICA = "legacy"`, so any
fresh hex replica does sort before `"legacy"` — the "legacy-replica ordering
trap" #334 documents as Open Question 5 is real. But Open Question 5 itself
is marked "does not block filing," and #436's own Hypothesis H2 (whether the
per-block `int id` line stays position-dependent even after references move
to `sref`) is marked "blocks execution *if* H2 is confirmed" — i.e., neither
issue guarantees the third class actually passes; both defer the decision.
If H2 is confirmed and Open Question 5's alias table is not built, the
`legacy`-sorting class of §1.3 is not satisfiable by the described mechanism
alone, and #334's Definition of Done would have to be met by re-scoping
rather than by the plan as written.
**Recommendation:** either promote the legacy-replica alias table (Open
Question 5) to a blocking prerequisite of #334's own DoD, or explicitly
narrow §1.3's "every replica-id class" claim until H2/Q5 are resolved.

**6. (Low) Proportionality / process cost.** This is a ~2 task, "2-4
maintainer-week" feature carrying roughly 900 lines of YAML machine blocks,
mermaid graphs, LaTeX transformation proofs, and cross-references into six
other multi-thousand-word issues (#315, #319, #356, #436, #437, #488) that a
contributor must partially internalize to safely act on it. ARCHITECTURE.md
records this project's own stated bias against exactly this kind of
per-feature ceremony tax ("single-maintainer pedagogy tool... a large,
ongoing tax with no requesting user" — said of i18n, but the principle
generalizes). Not a defect in the technical content, but worth naming as a
contributor-accessibility cost that the issue itself never weighs against
its benefit.

## What's solid

- The two-task split (reference form vs. container default) is well-justified — §2's rejected alternatives are specific and technically sound (an
epoch-compatible reader is genuinely not separable from the reference-form
work).
- Global Invariants (legacy files still load, byte-identical saves unless a
version bump is declared, `MAX_CIRCUIT_TEXT_BYTES` untouched, `mvn verify`
green) are concrete and testable, and match what I found in
`src/jls/FileAbstractor.java:65` and `src/jls/Circuit.java`.
- The `blocked_by`/`blocks` edges to #315, #319, #356 are genuinely mirrored
in both directions — I fetched all three and confirmed each names #334 back
correctly (`#319 blocked_by: [334]`, `#356 blocked_by: [319, 334]`,
`#315 blocks: [..., 334, ...]`), so the DAG claims in this issue are not
one-sided fabrications, unlike the dangling commit citations.
- The out-of-scope list (bulk payloads → #319, semantic merge safety → #356,
op-based CRDT → #171) draws real, defensible boundaries rather than vague
hand-waving, and each boundary is backed by a concrete ownership statement
in the corresponding issue.
