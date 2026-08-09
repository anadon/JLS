# Issue #337: FEAT-015 (RESIDUAL): every circuit mutation applies with no windowing system present, and a program can build a circuit without emitting save text
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

The issue is unusually well-grounded: every file/line citation I spot-checked
against the repo at HEAD matches (the 11 `apply(Circuit, Graphics)` sites,
`CircuitOp.java:34-37`, `TextMetrics.java:13-19`, the `SimpleEditor`
`markChanged()` line list, `CircuitTextBuilder`'s 461 lines and 24 test
consumers, `docs/operation-layer.md`'s inline-gesture inventory). The scope
boundary against #316 (mouse-state-machine) and #167/#282/#283 (gesture
migration) is coherent and cross-checks cleanly against those issues' own
text. That said, the issue has one confirmed factual defect (a stale
dependency graph pointing at closed duplicate issues) and one confirmed
internal contradiction (a claim the issue's own comment thread explicitly
refutes but the body was never edited to remove), plus several softer
concerns below.

## Findings, most severe first

### 1. [HIGH] The machine block's `blocks` list names two issues that are now closed as duplicates, and the body was never updated

The YAML in the issue body reads `blocks: [316, 318, 326, 345, 348, 352]`
and the mermaid graph draws edges to `N348["#348 FEAT-051 P2P session
foundation"]` and `N352["#352 FEAT-052 CRDT replication, undo and
hardening"]`. I fetched both: **#348 is `state: closed,
state_reason: duplicate`** and **#352 is `state: closed,
state_reason: duplicate`** (both closed 2026-08-04, by anadon). The issue's
own comment (`issuecomment-5176102259`, posted the same day by the issue
author) says so explicitly and prescribes the fix: `#348` → re-point to
`#169`, `#352` → re-point to `#171`, giving `blocks: [316, 318, 326, 345,
169, 171]` — "still six, none dropped." The comment also states plainly
"the machine block is the maintainer's to edit," meaning the correction is
known and intentionally deferred, not an oversight nobody noticed. As of
this read, the body **has not been edited**: the YAML, the prose ("blocks:
[316, 318, 326, 345, 348, 352]"), and the mermaid diagram all still name the
dead issues. §7's own re-planning protocol calls a mismatch like this "a
half-edge... the defect this Link pass exists to prevent" — that defect is
live in the current body. **Recommendation:** before filing TASK-0037/0038,
edit the body's machine block, the six-features sentence, and the mermaid
graph to the corrected `[316, 318, 326, 345, 169, 171]`, or an implementer
following the body verbatim will plan a `STATUS:` handoff to two closed
issues.

### 2. [HIGH] The body asserts a disposition for #167 that the issue's own comment thread explicitly refused

"Related work already in the tracker" says: *"#167 should close under this
feature's landing, not alongside it."* The same comment (`#337#issuecomment-5176102259`,
§2) directly rules this out: *"This issue's § Related work proposes that
#167 'should close under this feature's landing'. That proposal is
refused — neither of this feature's two outcomes appears in #167's scope,
criteria or roster, so #167 is not a duplicate of it, and #167 is
additionally protected as a declared prerequisite of #171 (`blocked_by
[167, 168, 169]`), #223 and #316."* I confirmed independently: #167 is open,
carries `requires_tasks: [282, 283]` (both open), and is a named prerequisite
elsewhere. The body's narrative claim is therefore not merely optimistic —
it was adjudicated and rejected by the same author in the same issue's
comment stream, and the body text was left standing uncorrected. A reader
who stops at the body (as most implementers will) picks up a wrong
disposition for #167 and may prematurely close it or defer #282/#283 work
on the assumption #337 subsumes them. **Recommendation:** strike or correct
the "#167 should close under this feature's landing" sentence to match the
comment's adjudication (overlap noted, not merged; #337 narrows to the
signature substitution, #282/#283 stay filed under #167).

### 3. [MEDIUM] "Both scopes verified ABSENT at `2d0ca9d`" overstates how absent the headless path actually is

The issue's evidentiary center is the claim that no headless/`TextMetrics`
work exists yet at the op boundary — "Both scopes verified ABSENT at
`2d0ca9d`." I checked the actual call sites:

- `SetElementConfig.validate` (`src/jls/collab/op/SetElementConfig.java:98-121`)
  already has a working nullable-`Graphics` branch: `private Element
  validate(Circuit circuit, @Nullable Graphics g)` with `if (g != null) {
  fresh.init(SwingTextMetrics.forGraphics(g)); }` — i.e. this op kind
  **already runs its validation path with `g == null`** today, skipping the
  geometry re-init rather than requiring a live `Graphics`.
- Every op that touches element geometry (`AddElements.java:61`,
  `FlipElement.java:26`, `RotateElement.java:31`,
  `SetElementConfig.java:66,120`) converts its `Graphics` to `TextMetrics`
  immediately via `SwingTextMetrics.forGraphics(g)` before calling into
  element code — the element hierarchy itself (`Element.init`, `src/jls/elem/Element.java:415`)
  already takes `jls.core.@Nullable TextMetrics`, not `Graphics`, at all.

So the "residual" is not a discovery-heavy migration through unmapped
territory — it is closer to deleting the outer `Graphics` parameter,
threading `TextMetrics` straight through `CircuitOp.apply`, and deleting the
now-redundant `SwingTextMetrics.forGraphics` conversions at 11 call sites.
That is consistent with the issue's own "mechanical substitution... weeks
not months" framing, but it sits oddly next to "verified ABSENT": the
nullable path for at least one op kind is not absent, it is implemented and
presumably already exercised by whatever calls `SetElementConfig.validate`
with `g == null` today (the `invert()` path, per its javadoc). This doesn't
break the issue, but it means the "residual" is measurably smaller than the
dramatized framing suggests, and a reviewer should treat the 2-week TASK-0037
estimate as comfortable rather than tight. **Recommendation:** note in the
issue that the `TextMetrics` plumbing already reaches every op's geometry
call sites, and scope TASK-0037 explicitly as "delete the `Graphics`
parameter and its per-op conversions," not as a metrics-abstraction design
task — that would also make the 4-7 mw band easier to defend as a ceiling
rather than a floor.

### 4. [MEDIUM] Criterion 2 ("deferral list is empty or reviewed and shrinking") is gameable by reclassification, not just by real closure

§5 criterion 2 says a passing state is "every one of the 13 mutation-marking
call sites... is either behind `OpSink` or on a committed list with a named
reason and a successor issue. A list that grew since the previous landing is
a `REPLAN:`, not a pass." But §7's own "Scope bleed into #316" clause
explicitly sanctions moving a gesture's closure obligation to #316 with a
`HANDOFF:` comment. Nothing stops TASK-0037 from satisfying criterion 2 by
handing several of the 13 sites to #316's deferral list rather than actually
closing them through `OpSink` — the count at #337 shrinks, the count at #316
grows, and criterion 2 reads green while the actual headless-application
capability (the thing #300/#304/#171 need) has not moved. The issue does not
require the *aggregate* inline-gesture count across #337+#316+#167 to shrink,
only #337's own list. **Recommendation:** state explicitly that a HANDOFF
to #316 does not count toward criterion 2 unless #316's own closure is also
tracked as a joint metric, or the criterion is trivially satisfiable by
relabeling.

### 5. [MEDIUM] Criterion 5's "nontrivial circuit" is undefined and the golden-comparison direction is easy to violate silently

"A nontrivial circuit built by the verbs matches a committed golden, and the
golden was produced by the GUI path, not by the verbs — otherwise the test
compares the verbs against themselves." Good instinct on the second half,
but "nontrivial" has no size/element-count/feature-coverage floor. A golden
with two gates and one wire technically satisfies the letter of criterion 5
while exercising almost none of the 11+ op kinds the verb set is supposed to
cover, and nothing in §5 or the Definition of Done catches that at review
time — it is a text description, not a testable predicate. Contrast with
how carefully the issue quantifies everything else (13 call sites, 24 files,
461 lines). **Recommendation:** pin "nontrivial" to something checkable —
e.g. "exercises at least N of the M op kinds" or "at least K elements
spanning gates, wires, and one stateful element" — the same rigor the rest
of the issue applies to its own evidence.

### 6. [LOW] Cost is committed ahead of a blocking open question

Open Question 4 ("Does the headless `TextMetrics` implementation ship in
main or test?") is marked "Blocks TASK-0037," yet § Cost prices TASK-0037 at
a firm 2 weeks and folds it into "the band's floor, not past it, so no
reconciliation is required." Pricing a task whose implementation location
(and therefore whether grading/import code can depend on it, per the
issue's own audience section) is still an open decision is optimistic
sequencing, not a defect, but it is worth flagging: if the recommended
default (`src/`) is contested at filing time, the 2-week estimate was set
before the design question it depends on was closed.

### 7. [LOW] Single-maintainer concentration risk understated

Six (now, per finding 1, effectively four surviving) downstream features —
#316, #318, #326, #345, #169, #171 — plus four capstones are gated on this
one 4-7 week band, in a project the README itself describes as "a
single-maintainer pedagogy tool" (ARCHITECTURE.md, README.md). §7 correctly
identifies the technical re-plan trigger ("a `TextMetrics`-inexpressible
geometry op") but the Cost section doesn't weight the blast radius of a
blowout against a single maintainer's calendar — a 2x slip here stalls
grading (#300), importers (#304), and the entire editor decomposition
(#316) simultaneously, not just this issue.

## What's solid

- The file/line evidence for the current state of `CircuitOp`, `TextMetrics`,
  `SimpleEditor`'s `markChanged()` sites, and `CircuitTextBuilder` all check
  out exactly against the repository at HEAD.
- The scope boundary against #316 (mouse-state-machine explicitly excluded)
  and against a public `jls.api` (explicitly excluded) is well-reasoned and
  matches #316's own text describing the same boundary from its side.
- The #167/#282/#283 relationship (this issue narrows to the signature
  substitution; #282/#283 own the gesture migrations, already filed under
  #167) is accurately described once the body is read together with the
  issue's own corrective comment — the underlying technical division of
  labor is sound even though finding 2 shows the prose wasn't reconciled.
- Global invariants (§4) — save-byte stability, historical-file loading,
  grammar strictness, inverse contract, `mvn verify` green — are concrete
  and testable, and mirror invariants already enforced elsewhere in the repo
  (e.g. `HeadlessCoreRatchetTest`, `CircuitOpTest`).
