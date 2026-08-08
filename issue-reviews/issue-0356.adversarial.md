# Issue #356: FEAT-012: a merged .jls file either means what both authors meant or is refused by name — no third outcome where it loads and simulates a circuit neither drew
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what's solid (won't repeat below)

- The core defect claim is real and independently reproduced against current HEAD, not just at the pinned evidence commit: the "bits don't match" width check is duplicated verbatim at `src/jls/edit/SimpleEditor.java:4014-4016, 4141-4149ish, 4246-4248, 4357-4359` (four editor-gesture call sites) and nowhere on the load path; `Circuit.finishLoad` (`src/jls/Circuit.java:1300-1320`) checks only stable-id uniqueness. A `git grep -in "duplicate name"` over `src/` returns exactly 10 lines, all in dialogs/paste guards, matching the issue's claim byte-for-byte.
- `sref`/`sprobe`/`MergeRules`/`ThreeWayMerge`/`jls.merge`/`SemanticCheck` are all confirmed absent from `src/` and `test/` at current HEAD, consistent with the issue's "absent, verified" claims.
- The decomposition rationale (validator is cheap/high-value, a merge driver is a standing bus-factor-1 maintenance surface, so they must be separately fundable) is a genuinely good engineering argument, not filler.
- Scope boundaries are drawn correctly: op-convergence semantics ("who wins") is explicitly kept out and assigned elsewhere; this issue owns only "which merged results are legal at all." That's a clean, defensible cut.
- `blocked_by: [319, 334]` is accurate: both #319 and #334 are open, and both are genuine technical prerequisites (section framing for merge-participation-per-section; stable-id references for merge rules to be expressible at all) rather than decorative edges.

## Findings, most severe first

### 1. The `blocks: [352]` edge is stale and currently false — the issue's own DAG hygiene rule is being violated in practice

The machine block states `blocks: [352]` and the prose/mermaid graph describe `#352 (FEAT-052)` as a live downstream consumer waiting on this feature ("`F12 --> N352`", "#352 waits on this feature, because a convergent replica produces files no single editor wrote..."). I fetched #352 directly: **it is closed, `state_reason: "duplicate"`**, closed 2026-08-04T07:47:21Z, superseded into #170 (hardening half) and #171 (replication half) per its own closing comment. #171, in turn, shows no `blocked_by`/`related` edge back to #356 in the body I fetched (its child task #279 lists only `[163, 166, 167, 170, 160]` under `related`).

This is not a stale artifact from before #356 was filed — #356's own comment thread continued *after* #352 closed (dedup pass-2 comment at 2026-08-04T07:37:03Z, ten minutes before #352's closure; a further roster-addition comment as late as 2026-08-08T18:21:10Z) and neither caught it, because both are scoped to "cluster A" (core/save-format) and #352 sits in the collaboration cluster. The issue's own re-planning protocol states: *"a half-edge is the defect this Link pass exists to prevent."* That is exactly the current state of `blocks: [352]`.

**Recommendation:** before funding, correct the `blocks` edge — either point it at #171 (and #170, for the hardening half) with a fresh confirmation that #171 actually still needs #356's merge-rule table, or drop the edge and record why. Given #279 (now `part_of_feature: 171`) is the actual online-rules consumer #356's own text names as "the closest thing to a duplicate," the reconciliation obligation in §7 ("if #279 lands first...") is currently pointed at a dead node.

### 2. Five integration criteria have no assigned owner and are likely the unexplained cost gap

§5's Integration Criteria (the nine-scenario merge-safety matrix; record-kind totality test; bulk-section byte-identity test; git diff/textconv integration plus written limits; the full-corpus "accepted set shrank and did not move" aggregating run) are each stamped "*this issue's close-out owns it*" — but #356 is a coordination-tier feature issue, not an executable task with an implicit assignee. All three of its named children (#436, #409, #415) are independently scoped and do not claim this work in their own Definition of Done sections (I read #409 and #436 in full; neither lists the nine-scenario matrix or the totality-over-record-kinds test as their own deliverable).

Open Question 1 already flags an unowned cost residual (band 9-13 mw vs. printed task sum 5.5 wk, leaving 3.5-7.5 wk "with no task id"), but never connects that gap to these five specific deliverables. The issue is transparent that a gap exists but does not name what's actually in it, which makes the recommended-default "(b) re-measure before funding" hard to execute — there's nothing concrete to re-measure against.

**Recommendation:** either mint a fourth task (e.g. "TASK-0033: close-out integration suite") explicitly owning §5, or assign each §5 row to one of the three existing children and update their Definition of Done sections to match.

### 3. Several Definition-of-Done items are unfalsifiable process attestations, not tests

Example: *"Machine block, roster table, and mermaid graph agree with reality at close"* and *"#299 (CAP-01) notified with a STATUS: comment."* Nothing in the repo (that I found) mechanically checks that a YAML block agrees with "reality" — it's exactly the kind of drift finding #1 above demonstrates can and does happen silently even under this project's own heavy dedup-pass discipline. As written, a closer can satisfy this criterion by asserting it in a comment, which is gameable: the verification could pass while the actual DAG is wrong, which is precisely what has already happened with `blocks: [352]`.

**Recommendation:** if the DAG bookkeeping is meant to be load-bearing for scheduling (it clearly is, given how much of the issue body is DAG-walk prose), it should be checked by a script that parses every issue's machine block and verifies edge mirroring and open/closed state agreement — not left to per-issue prose review, which has already been shown to miss a cross-cluster closure.

### 4. Title's "no third outcome" framing is narrower than the design it's built on

The title states a merged file "either means what both authors meant, or is refused with a diagnostic" — binary. But §3's design explicitly has a third outcome: STRICT (offline `git merge-file`) is permitted to itself conflict (no merge product produced at all, handled entirely outside this feature by ordinary git conflict markers). The body text elsewhere states this more precisely ("either the merge conflicts, or the merged file loads, validates ... or it is refused"), so this is a title-vs-body precision mismatch rather than a design defect — but worth tightening, since the title is what a skim reader (or an automated tooling pass) will index on.

### 5. The #279 reconciliation obligation is one-sided

§7's inversion clause ("if #279 lands first, TASK-0032 becomes the STRICT column plus a reconciliation that AUTO equals #279's shipped behaviour") is stated only on #356's side. I read #279's full body: it carries no corresponding edge or note obligating it to check back against #356/#415's STRICT table before shipping its AUTO rules. Combined with finding #1 (the #352 node that used to carry this cross-reference is now closed), there is currently no mechanism that would stop #279 landing first and #415 quietly staying unreconciled — "do not file a third rule table" is a promise with only one enforcer left standing, and that enforcer (#356) will not itself execute unless someone remembers to look.

## What would make this sound

Fix the `blocks` edge (finding 1), assign an owner to §5's five criteria (finding 2), and replace the two unfalsifiable DoD lines with a scripted crosslink check or drop them to "rides along" status. None of this blocks the three already-filed, independently well-specified children (#436, #409, #415) from proceeding — the coordination layer needs repair, not the underlying engineering plan.
