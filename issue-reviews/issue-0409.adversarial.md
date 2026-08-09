# Issue #409: TASK-0031: a file that parses but is structurally corrupt is reported by name, so a merge can never hand back a circuit nobody drew
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is unusually well-evidenced (line-pinned code excerpts, a
runnable repro for O2/O3, cross-references to a closed-issue taxonomy it
reuses), and the core defect (O1, the `if (loadPut != null)` block
mis-nested inside the `next:for` wire loop in `WireEnd.init`,
`src/jls/elem/WireEnd.java:102-104`) is real and verified against the
current checkout. But the issue ships with its single hardest design
decision unresolved and marked "blocks execution," its own machine-
readable dependency graph disagrees with its prose, one of its cited
"measured" facts is wrong, and its acceptance criteria for the
false-positive guarantee are weaker than the confidence with which they
are stated. None of these sink the task, but none should be picked up
as-is either.

## Findings, most severe first

**1. The flagship fix (P1/O2) cannot be built from the interfaces the issue actually specifies, and the issue says so itself without resolving it.**
§7.4 specifies exactly one new public surface: `jls.SemanticCheck` with
`List<Finding> check(Circuit c)`, precondition "`finishLoad` has
completed," postcondition "the circuit is unmodified." But O2's own
repro shows the information the flagship check needs — that wire end
`id 2` *declared* an attachment — is already gone from the object graph
by the time `finishLoad` returns: `getPut()` is `null`, `isAttached()`
is `false`, nothing distinguishes "never declared an attachment" from
"declared one and lost it." Open Question 1 admits this in terms: "the
flagship check cannot be written as specified" without either (a) a new
residue field on `WireEnd` that `init` must populate (a load-path change
to production code, not the check), (b) fixing the nesting bug in `init`
directly (explicitly rejected as "a bigger decision than it looks" that
changes load behavior), or (c) re-parsing file text (explicitly
rejected). Recommended default (a) is a real modification to
`WireEnd.init`/`Circuit`'s load path with its own new field, accessor,
and lifecycle — none of which appears anywhere in §7.4/§7.5, which
describe `SemanticCheck` as if it only reads a `Circuit`. This is not a
minor gap: it is the one check the issue calls "the flagship," and the
issue's own text marks the question "Blocks execution." A reviewer
should not accept a plan whose primary deliverable is architecturally
undecided at filing time.
*Recommendation:* resolve Open Question 1 before scheduling, and once
resolved, add the new `WireEnd`/`Circuit` surface it requires to §7.4/
§7.5 so the interface contract actually matches what P1 needs.

**2. The machine-readable dependency graph contradicts the prose, and the contradiction is still live.**
The `blocked_by` YAML field is `[]`, but the same block's inline comment
says TASK-0005 "is the one genuine ordering prerequisite" and the body
repeats "Depends on and does not build: the sref/sprobe item kind
(TASK-0005). P3 cannot be made green until it lands." At filing time
TASK-0005 had no issue number ("filed concurrently... a link pass adds
it"). It has since been filed as **#436** — confirmed by fetching it:
title "TASK-0005: inserting one element into a saved circuit...", state
`open`. #409's own 2026-08-04 follow-up comment even names it ("#409's
sibling #436 (TASK-0005)... #409 sits behind it on a three-link critical
path"), yet the issue body's `blocked_by: []` was never updated to
`[436]`. Any tooling that walks the dependency graph mechanically (as
#356's own DAG-walk section does for its children) will conclude #409
has no blockers, while the prose and the sibling comment both say
otherwise, and #436 is still open today. This is a live, not merely
historical, inconsistency.
*Recommendation:* update `blocked_by` to `[436]` before work starts, or
explicitly record the waiver-and-defer-P3 path the Method section
already offers as an alternative.

**3. A cited "measured" fact is wrong: O7 miscounts the flag table.**
O7 states "Fifteen flags at `2d0ca9d`; `check` is not among them," citing
`src/jls/JLSStart.java:759`. Counting `new FlagSpec(...)` entries in the
current `FLAGS` array (`src/jls/JLSStart.java:759-789`, which the issue
itself certifies as identical to `2d0ca9d` via an empty `git diff --stat`)
gives **14**, not 15: `h, b, i, s, t, d, p, v, r, vcd, export, board,
pins, savetext`. This is a small, inconsequential-to-the-outcome error,
but it matters for how much trust to place in the issue's other
"measured, not hypothetical" claims (its explicit standard, stated in
§3): if a single-array element count is wrong in a filing that treats
precision as its main credibility argument, the line-number anchors
elsewhere (O1-O6, O8-O10) need the re-verification the Method section
already mandates ("re-derive line numbers if HEAD has moved") applied
skeptically, not just mechanically.
*Recommendation:* re-count before use; treat every other pinned
citation as needing independent re-verification rather than trusting
the issue's arithmetic.

**4. The new `-check` exit status is a documented stability-contract change the issue treats as an afterthought.**
`docs/batch-interface.md` §1 is explicit: "**Status: normative, and a
stability contract**... any change to them requires a CHANGELOG entry
and either a major version bump or a compatibility flag that preserves
the old behavior," and its exit-code table currently has exactly three
rows (0/1/2). P7 and Open Question 3 both require a **new, distinct**
exit status for "loaded with findings" vs. "load failed" — necessarily
a fourth row. Open Question 3 calls this "Rides along; record it in
`docs/batch-interface.md` §1" as if it were a documentation footnote,
but by the document's own stated policy this is exactly the kind of
change that needs a CHANGELOG entry and a version bump/compat flag. The
Definition of Done (§14) never mentions either. As written, an
implementation could add exit code 3 and update the flag table without
tripping any of the stated completion criteria, which is a formal pass
against a real regression in the batch-interface stability contract.
*Recommendation:* add the CHANGELOG/version-bump obligation explicitly
to §7.1 and to the Definition of Done checklist, not just "rides along."

**5. H2/P5's zero-false-positive guarantee is asserted with more confidence than the corpus supports, and the harder oracle lives in a different, unbuilt issue.**
The corpus for "zero findings on clean input" is three tracked fixtures
(O9) plus "every importer/emitter output" — the issue's own Threats to
Validity §11 already flags this ("The corpus is three files... A false
positive on a file class not represented there survives H2"). More
importantly for gameability: H1's falsification criterion is stated
against "a scenario in #356's merge-safety matrix" — but that matrix
("table-driven over at least nine scenarios," #356 §5 item 1) is
explicitly **not** this task's deliverable; it belongs to #356's own
close-out, which in turn is gated on TASK-0032 (a third, also-unfiled-
at-evidence-time task). #409's own Definition of Done (§14) never
requires running the six checks against that nine-scenario matrix — only
against the three fixtures and importer/emitter output. So an
implementation can satisfy every literal completion-criterion checkbox
in #409 while never being tested against the harder adversarial
scenarios (independent width edits, insert-early vs. wire-spares, etc.)
that #356 itself lists as the real bar. The acceptance criteria, as
scoped to this issue alone, are gameable by overfitting to the four
hand-built `SemanticCheckTest` cases (P1-P4) and the three fixtures.
*Recommendation:* either pull a minimal slice of #356's merge-safety
matrix into #409's own Data Collection & Analysis section (§9) as a
required test, or explicitly state in the Definition of Done that P5's
guarantee is provisional pending #356's matrix and must be re-verified
there — right now it silently reads as final.

**6. Definition-of-Done items whose evidence lives outside this issue's own artifact are not independently verifiable.**
Two DoD bullets ("Landing reported on #356 with a `STATUS:` comment,
**including O4**" and the Method-section step "Post a `REPLAN:`-worthy
note on #356 recording O4") make part of #409's completion consist of
prose posted to a *different* issue's comment thread. `mvn verify`
cannot check this, and neither can any test in §9. It is a legitimate
coordination need (O4's contract deviation genuinely belongs on #356),
but as a completion criterion it is unfalsifiable from inside #409 —
a reviewer has to go check #356's comments to know if #409 is "done" by
its own stated bar.
*Recommendation:* keep the coordination requirement, but phrase it as
"the PR links a #356 comment containing X" so it is checkable from the
PR itself rather than requiring a separate audit of another issue.

**7. Open Question 2's "blocks execution if the per-undo cost turns out to matter" has no stated threshold.**
"measure it before deciding" with no number attached means any measured
cost can be waved through as "not mattering." Given `SemanticCheck` is
recommended to run on every `CircuitSnapshot` restore (i.e., on every
undo/redo step, potentially per keystroke-adjacent gesture in the
editor), this is exactly the kind of unbounded acceptance criterion the
adversarial lens should flag: it cannot fail a review because nothing
was promised to fail against.
*Recommendation:* state a concrete latency budget (e.g., "p99 added
latency per undo on the largest tracked fixture must stay under N ms")
before the open question is considered resolved.

## What's solid (brief)

- O1's core defect (mis-nested `if` inside the wire-loop `for`) is real, reproduced, and matches current source exactly at `src/jls/elem/WireEnd.java:94-131`.
- O5's three existing throws and the duplicate-`sid` rejection are accurately cited (`WireEnd.java:105-127`, `Circuit.java:1310-1320`) and P8's "these must stay load failures" framing is a sound scope boundary.
- O6's catch-all warning (`Circuit.java:1401`, `catch (Exception ex)`) accurately identifies a real hazard for routing findings.
- Explicitly deferring multi-driver legality to `docs/simulation-semantics.md` rather than re-specifying it (§7.10) is the right call and avoids a real drift hazard.
- Reusing `LoadError`'s taxonomy shape and the closed-issue precedents (#58, #38) is appropriately scoped prior art rather than reinvention.

## Verdict rationale

`needs-rework`: the core defect is real and the observation quality is
mostly high, but the flagship fix's design is explicitly unresolved and
marked blocking, the dependency graph is internally inconsistent in a
way that is still live against the actual tracker state, and the
acceptance criteria for the headline false-positive/false-negative
guarantees are narrower than their stated confidence — all fixable by
edits to the issue rather than requiring the whole task to be
re-conceived, hence not `should-not-proceed`.
