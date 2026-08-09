# Issue #167: Operation layer: reify editor mutations as invertible, serializable commands behind one entry point (collab Stage 0b)
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of what I checked

This is a restructured "feature-tier" tracking issue with 15 comments spanning three weeks
and 7+ merged PRs (#194, #196, #239, #247, #262, #273, plus #263 for PIT thresholds). I spot
checked its most load-bearing citations against the checked-out tree rather than taking them
on faith:

- `CircuitOp.java:34-37` permits list — matches exactly (11 sealed kinds).
- `OpSink.java:24` (`submit`) and `:42` (`submitAll` default) — match exactly.
- `OpExtensionPoints.java:26` — matches.
- `SimpleEditor.java` is 5,852 lines; `markChanged()` appears at exactly the 16 lines the
  issue/comments cite (749, 1402, 2560, 2819, 2873, 2929, 3439, 4964, 5105, 5244, 5497, 5500,
  5552, 5557(comment), 5569, 5754) — 13 real call sites, matching the comment's count.
- `moveSelectionPlan` is at `SimpleEditor.java:1053-1116` exactly as cited.
- `CircuitOpTest.java` has 41 `@Test` methods and `rejectionsLeaveTheCircuitUnchanged` is at
  line 761, as cited.
- `ArchitectureRulesTest.collabLayersAreHeadless` exists at line 150, as cited.
- `#165` and `#166`, listed as `blocked_by` and asserted "LANDED (closed)", are both confirmed
  `state: closed, state_reason: completed`.
- `#282` and `#283`, the two `requires_tasks`, are both confirmed `state: open`, correctly
  scoped and each carries `part_of_feature: 167`.

This is an unusually well-evidenced issue — permalinked line numbers were not fabricated or
stale. That raises the bar for what counts as a real finding here; the problems below are
process/structural, not citation fraud.

## Findings, most severe first

### 1. Closure authority is asymmetric and contradictory between #167 and #337
`#337`'s Definition of Done contains the line: *"#167 dispositioned — closed as completed by
this feature, or its residual named"* and its own body states *"#167 should close under this
feature's landing, not alongside it."* But #167's own Completion Criteria make **no mention of
#337 at all** — its DoD is self-contained (children #282/#283 closed, §5 integration criteria
verified, capstones #163/#224 notified). That means #167 could satisfy every checkbox in its
own DoD and be "ready to close" while #337 has not yet decided whether #167 gets closed by
#337's landing or stays open with a named residual. Two issues each act as if they hold the
authority to decide #167's fate, and neither DoD defers to the other's. **Recommendation**:
either add an explicit line to #167's DoD ("do not close independently of #337's disposition")
mirroring what #337 already states unilaterally, or drop #337's claim on #167's closure and let
#167's own I1-I4 criteria be sufficient.

### 2. Real code-collision risk between #282/#283 and #337's (unfiled) TASK-0037
The three most recent comments (5175848440, 5181638045, 5227023243 — three separate "boundary
record, not merged" comments in 5 days, Aug 4/4/8) all converge on the same fact: #282 and #283
migrate the *same four call sites* (placement drop, paste, wire-attach finish, dialog commit)
that #337's TASK-0037 also claims as "criterion-3 gestures." #337 itself names the collision
point explicitly: *"provided the `OpSink` adapter at `SimpleEditor.java:5497` is not being
rewritten at the same moment."* That is a real merge-conflict / rework risk between two
differently-scoped, differently-timed, currently-unfiled-or-unassigned pieces of work (#282,
#283 are filed and open; TASK-0037 is only "planned," not filed) with no enforcement mechanism
beyond a comment asking people to "coordinate." If TASK-0037 gets filed and picked up by a
different agent/PR while #282/#283 are in flight, the `Graphics`-signature change and the
gesture migration will very plausibly touch the same lines twice. **Recommendation**: file
TASK-0037 now (or block it) with an explicit `blocked_by: [282, 283]` rather than "concurrent,
coordinate" — the sequencing note in #167's own §6 ("Sequencing & Parallelism") does not mention
#337/TASK-0037 at all, even though it is the one external actor most likely to collide.

### 3. I1 (inventory closure) is gated on a decision #167 cannot make and has no owner or timeline
The Open Questions section states the ordered-row vocabulary question "**Blocks filing the
ordered-rows child**" and is to be "decide[d] with Stage 2 (#163)." #163 is itself a large
open tracking issue (pure-P2P collaborative editing) with no visible timeline for a "Stage 2"
decision. Yet #167's own Completion Criteria require `planned_tasks` to be **empty** (each
resolved to a filed issue or descoped via REPLAN) before close, and I1 requires "no gesture
still inline" row including ordered-row edits' disposition. This makes #167's close date
contingent on a decision inside a different, larger, indefinitely-open issue that #167 has no
control over — an unbounded external dependency stated nowhere in the `blocked_by` field (which
lists only `[165, 166]`, both already closed). **Recommendation**: either add `#163`
(or a Stage-2-decision placeholder issue) to `blocked_by` honestly, or decouple I1's "no
un-owned inline row" bar from a decision that has no committed timeline.

### 4. The code has arguably already answered the "Open Question" the issue still treats as blocking
`SetElementConfig.java:19-30`'s javadoc states in the present tense: *"the commit-time op behind
every attribute dialog and every ordered-row editor (state machine, truth table, signal
generator) ... 'set-attribute' and 'edit-ordered-rows' are one commit-time op kind, not
several."* That is a declarative, already-shipped design decision. Yet the issue's own Open
Questions section still frames this as unresolved ("distinct `EditOrderedRows` kind ... vs
whole-block replace via `SetElementConfig`, whose javadoc already claims...") and the machine
block still carries "Ordered-row edits ... as ops — gated on the Stage 2 sequence-semantics
decision" as a `planned_tasks` entry blocking I1 (see Finding 3). If the javadoc is authoritative,
the "decision needed" framing and the Stage-2 gate are stale busywork; if the javadoc is
aspirational/wrong, that is a doc/code mismatch nobody has flagged as a defect. Either way this
is unresolved documentation debt masquerading as an open design question.
**Recommendation**: a REPLAN comment that either retires the ordered-row planned-task line
(citing the javadoc as the already-made decision) or corrects the javadoc's overreaching claim.

### 5. No cost/size estimate anywhere in #167, unlike its sibling #337
#337 (a smaller-scoped feature by its own admission — "a residual band") carries an explicit
`## Cost` section: "Band: 4-7 maintainer-weeks" with a stated basis. #167 — which has already
consumed 7+ PRs and three weeks of wall-clock time and still has two open children plus two
unfiled planned tasks — states no cost estimate at all, not even retroactively. For a
feature-tier issue with this much process overhead (mermaid graphs, machine blocks, REPLAN
protocol, multiple boundary-record comments), the absence of any sizing information for the
remaining work (#282, #283) makes it hard for a reviewer or scheduler to judge whether "the
critical path to close: #282 (largest surface)" is a day or a month. **Recommendation**: add a
`## Cost` section mirroring #337's format, at minimum for the two open children.

### 6. Verification criteria are entirely self-attested, with no independent gate
Every prediction in §5 and every DoD line resolves to "recorded in a closing comment" /
"command and output recorded in a PR" / a `STATUS:` comment. There is no CI badge, no reviewer
sign-off requirement, no second-party check named anywhere in #167's own text — `mvn verify`
green is asserted by the same actor (an OWNER-authenticated but AI-generated comment stream,
"Generated by Claude Code" on every comment) that also authored the migration. This is not
unique to #167, but it is worth flagging explicitly for a mutation-seam feature whose entire
value proposition rests on "atomic rejection" and "byte-exact inverses" — properties that are
easy to assert and comparatively expensive to independently re-verify. A closing comment that
merely restates the DoD checklist as done would satisfy every gate in this issue's text without
an outside party re-running anything. **Recommendation**: at minimum, require the closing
`STATUS:` comment to paste the actual `mvn verify` / PIT output rather than an English summary
that it passed — which several of the comments already do (e.g. "859 tests," "41 @Test methods")
but not all, and the DoD does not make this mandatory for the parent issue's own close, only
implicitly by convention.

### 7. Scope-boundary churn as a soft feasibility risk
Four of #167's 15 comments (5175848440, 5181638045, 5227023243, plus the REPLAN at 5154417305)
are dedicated entirely to re-litigating "is this a duplicate of #337?" without landing any new
disposition beyond "not merged, both stay open" each time. Three of those four land within a
single 5-day window (Aug 4-8) restating substantially the same conclusion. That is weak evidence
of genuine boundary instability — every fresh dedup pass re-derives the same split from scratch
rather than referencing (and trusting) the prior boundary comment, which suggests the scope line
is not actually settled in anyone's durable memory/design doc, only in comment threads that a
future pass may re-litigate a fourth time. **Recommendation**: promote the boundary to
`docs/operation-layer.md` or a short ADR so future dedup passes read one canonical statement
instead of re-deriving it from the issue history each time.

## What is solid (no further action needed)

- The technical contract (§3/§4: validate-then-mutate, byte-exact invert, atomic rejection,
  headless layering enforced by `ArchitectureRulesTest`) is precise, testable, and — as checked
  above — actually implemented and tested at the cited line numbers.
- The dependency graph (`blocked_by: [165, 166]`, both closed; `requires_tasks: [282, 283]`,
  both open and correctly scoped) is internally consistent and matches live GitHub state.
- The historical PR trail (#194, #196, #239, #247, #262, #263, #273) is verifiable and each
  landing comment's claimed artifacts (test files, line counts) check out against the tree.
- The "planned tasks stay unfiled rather than filing unexecutable work" discipline (subcircuit
  import, ordered-row edits) is a reasonable practice in isolation — the concern is only that
  I1's closure bar treats them as blocking with no owner (Finding 3).

## Verdict rationale

The engineering substance is sound and unusually well-verified against the actual codebase. The
concerns are all in the meta-layer: an unresolved authority conflict with a sibling feature
issue over who gets to close #167 (Finding 1), a real code-collision risk between two
independently-scheduled efforts touching the same lines (Finding 2), an open-ended external
blocking dependency not declared in the machine-readable `blocked_by` field (Finding 3), stale
"open question" framing that the shipped code already answers (Finding 4), and missing sizing
information (Finding 5). None of these block the two filed children (#282/#283) from proceeding
on their own technical merits, but they are real risks to #167 ever reaching a clean close.
