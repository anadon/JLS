# Issue #279: Simultaneous editing: per-kind confluent CRDT merge rules + P1 in-process convergence suite (collab Stage 2 slice)
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

This is a well-evidenced task issue — its code citations (`CausalBuffer.java`,
`OpEnvelope.java`, `Circuit.stateHash()`, the `OpSink`/`AddWire`/`NetBlocks`/
`MoveElements` references, the `collabLayersAreHeadless` architecture rule, the
22-test/40-permutation cycle-1 claim) all check out verbatim against
`master`. The scope boundary, DAG hygiene, and headless/no-new-dependency
constraints are consistent with the codebase and with parent issue #171. That
said, three things need fixing before or during pickup: a mischaracterized
citation to the research doc, a self-acknowledged but unapplied cross-issue
correction already sitting in the issue's own comment thread, and a gap in
the merge-rule set for a concurrent scenario the issue claims to cover.

## Findings, most severe first

### 1. The issue's own most recent comment says the machine block is wrong, and it hasn't been fixed

The single existing comment (posted 2026-08-08T17:47:31Z — the same instant
as the issue's `updated_at`, meaning the body was *not* actually edited when
the comment landed) is itself an adversarial review that found #279 and #415
both author "per-kind merge rule tables" with no edge between them, and
concludes:

> "Corrected here: `blocks: [415]` ... Mirrored on #415 as `blocked_by`
> gaining #279."

I verified independently: #415's machine block (`related: [352, 334, 319,
171, 170, 167, 163, 166, 165, 78]`) still does not name #279, and #279's own
body still reads `blocks: []` and `related: [163, 166, 167, 170, 160]` — the
correction was proposed but never landed in either issue body. A contributor
who reads only the machine block (as the DAG-safety walk explicitly invites:
"Cycle walk for DAG safety...") will conclude #279 has no downstream
consumers outside #171's roster, when the repo already contains a second,
undated issue (#415) whose entire justification (its H3/P8 cross-check)
depends on #279's §7.4 API existing first. Anyone picking up #279 without
reading its comments will not discover this.

**Recommendation:** apply the comment's proposed edit to both issues'
machine blocks before work starts, or explicitly reject it with a reason.
As filed, the issue is internally inconsistent with itself.

### 2. "Delete-wins over concurrent attribute edits" is attributed to a research-doc table that doesn't state it

The abstract and H1 both claim the merge rules — including "delete-wins over
concurrent attribute edits" — come from "the research doc §3 table". I read
`docs/collaborative-editing-research.md` in full: the §3 table (lines
239–244) has exactly three relevant rows — add-wins element set, per-attribute
LWW register, OR-set wires — and a case-insensitive grep for "wins" or
"delete-wins" anywhere in the document turns up nothing resembling a
delete-vs-attribute-edit rule. The "delete-wins" clause is a real design
decision (it also appears in #171's decomposition table), but it is not
sourced from where the issue says it is. This matters because §3 is cited as
*the* authority the executor should build against — an executor who goes to
verify the citation and finds it partially absent may either (a) invent their
own rationale for the missing rule, or (b) waste time hunting for a table row
that isn't there.

**Recommendation:** either amend the research doc to add the missing row (if
this is truly settled design) or correct the issue text to say the rule is
adjudicated in #171/#163 rather than "the research doc §3 table."

### 3. H1's four named rules don't obviously cover cross-kind conflicts, despite claiming to cover "the landed op vocabulary"

H1 lists four rule shapes: add-wins element set, delete-wins over concurrent
*attribute* edits, per-attribute LWW, OR-set wires. But the landed op
vocabulary (`CircuitOp`'s eleven permitted records, confirmed in
`src/jls/collab/op/`) includes `AddWire`, which attaches to an `ElementId`,
and `RemoveElements`. None of the four rules states what happens when one
replica concurrently removes an element while another replica adds a wire
attached to that same element's put — this is a *cross-kind* conflict (set
membership vs. OR-set wire), not covered by "add-wins element set" (about the
element itself) or "delete-wins over attribute edits" (about attributes, not
wires) or "OR-set wires validated against put existence" (validates against
put existence at apply time on one replica, but doesn't say who "wins" when
the two ops are concurrent and delivered in different orders on different
replicas — one replica may see delete-then-wire-add and reject the wire,
another may see wire-add-then-delete and accept it before delete arrives,
producing a real divergence risk that P2's stateHash() equality check exists
to catch, but the *design* section does not name this schedule as a decided
case). §11 (Threats to Validity) hand-waves this as "the vocabulary is still
growing... these rules cover the landed kinds", which begs the question since
`AddWire`+`RemoveElements` concurrency is already in the landed kinds today.

**Recommendation:** name the element-delete-vs-wire-add (and
element-delete-vs-move, if not already subsumed by "attribute") interaction
explicitly as a fifth rule (or explicitly fold it into "delete-wins", stated
precisely) before the property suite is written, not discovered as a shrunk
counterexample during implementation — the issue's own §10 (Falsification
Criteria) treats a shrunk counterexample as evidence the *design* is
unsound enough to escalate to #171/#163, which is an expensive way to learn
something a design review could catch now.

### 4. The CI trial-count gate is an unresolved Open Question, which weakens Completion Criterion "Convergence suite in CI with recorded seeds"

P2 requires "≥10^4 seeded trials" for the headline confluence claim, but
§8's last method bullet and the Open Questions section both punt the actual
PR-blocking trial count to "executor's choice ... rides along" ("bounded PR
lane; full count nightly if PR CI time demands"). The Definition of Done only
requires "`mvn verify` green" and "Convergence suite in CI with recorded
seeds" — neither of which pins a minimum trial count for the blocking lane.
This is a real gameability gap: a PR could land with, say, 50 trials in the
blocking lane (technically satisfying every literal DoD line item) while the
10^4-trial claim only gets exercised nightly, off the merge-blocking path,
so a regression introduced by a later PR could sit unnoticed until the next
nightly run finds it — precisely the "silent divergence" harm the issue
exists to prevent.

**Recommendation:** either pin a minimum PR-lane trial count in the DoD
(e.g., "≥N trials in the blocking lane, ≥10^4 nightly") or make "full P2 run
green at HEAD" an explicit pre-merge gate rather than a nightly-only check.

### 5. P3 ("every envelope crosses the real frame path even in-process") is asserted only by the harness the same task builds

P3 says this property is "harness-asserted", but the harness itself is new
work item in §8 ("In-process N-replica harness with delivery-order
permutation... to build"). There's no independent check (outside this task)
that the harness actually routes through `OpEnvelope`/`CausalBuffer` rather
than, say, calling the per-kind merge functions directly on in-memory op
lists for convenience — which would make P2's "confluence" result true of the
merge functions in isolation but say nothing about the real envelope/causal-
delivery path the production system will use. Nothing in the Completion
Criteria requires an independent reviewer or a separate test to confirm the
harness didn't bypass the frame path; it's self-graded by the same PR.

**Recommendation:** make "harness bypasses `OpEnvelope`/`CausalBuffer`" an
explicit anti-pattern called out in the Method or Threats section (the way
§3's "anti-pattern to name and avoid" text does for text-diff merging in the
research doc), or add a dedicated assertion (e.g., instrumenting
`CausalBuffer.deliver` call counts) rather than relying on code review to
catch a shortcut.

## What's solid (no action needed)

- All file/line/commit citations verified against the actual repository
  state at the cited commit and at HEAD — no stale or fabricated references.
- Scope boundary (no `.jls` format change, no new dependency, headless-only)
  is consistent with `ArchitectureRulesTest.collabLayersAreHeadless` and with
  #171's Global Invariants.
- The DAG-safety walk in the machine block is correct as far as `blocked_by`
  is concerned: #279 genuinely has no listed blockers and none of its cited
  prerequisites (#247/#262/#273) point back to it.
- The falsification criterion (§10) correctly ties a failing schedule to a
  concrete corrective action (fix the rule, add the regression) rather than
  leaving pass/fail ambiguous.
- P4 (semantically invalid remote op rejected, state unchanged) is concrete
  and testable against existing `OpEnvelopeTest`-style hostile-input coverage
  patterns already in the codebase.

## Verdict rationale

Not `needs-rework`: the core hypothesis, apparatus, and most of the contract
are sound and clearly grounded in the actual code. Not `sound`: finding #1 is
a live, already-identified inconsistency between the issue and its own
comment thread that the issue text has not absorbed, and finding #3 is a real
design gap in the rule set the issue claims is complete for the landed
vocabulary. `sound-with-concerns` — proceed, but resolve #1–#3 (and ideally
#4) before or very early in implementation.
