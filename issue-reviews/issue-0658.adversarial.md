# Issue #658: TASK-C566-2: every named FSM gap is closed or refused in writing — no item leaves the assessment without a disposition
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the issue

#658 is TASK-C566-2, the second of four tasks under feature #566 (FEAT-C31-4,
FSM parity with Digital/DEEDS/Issie), itself under capstone #515 (CAP-31). Its
job is to take a "gap list" produced by TASK-C566-1 (#657) and give each
numbered gap a disposition: closed by a PR-sized change, or refused with a
named reason and a filed follow-up task under #566. AC-4 additionally
requires writing the dispositions back into #657's document.

## Findings, most severe first

### 1. Hard dependency on an artifact that does not exist yet — the task cannot start, and its own sizing claim is unverifiable
`ordering_after: ["TASK-C566-1 (the gap list this disposes of)"]`. I checked
the repo: there is no `docs/` file enumerating FSM-workflow gaps, and #657
(TASK-C566-1) is itself open and unstarted — it has zero comments and its own
body says "Assessment only." `find`/`ls docs/` turned up nothing matching
`fsm`, `state`, `c566`, or `c31`. So #658 is currently pure specification
with no input to act on. That by itself is normal (task ordering exists for
exactly this), but the issue compounds it with a self-contradictory claim:
"This task deliberately cannot be sized precisely before TASK-C566-1 lands;
the AC-2 rule is what keeps it to one PR's worth of work rather than
absorbing an unbounded amount." AC-2 is not a sizing mechanism, it is a
routing rule ("any gap larger than that is filed as its own task") — it
bounds *individual gaps*, not the *sum* of all gaps disposed of in this one
PR. A gap list with, say, twelve small-but-real gaps could still make "one
PR's worth of work" balloon well past what a reviewer can sanely review in
one pass, and nothing in AC-1–AC-4 caps the *count* of gaps closed here.
**Recommendation:** before work starts, either cap the number of gaps this
task closes (e.g. "closes at most N gaps; the rest refuse-and-file") or drop
the "keeps it to one PR" claim, which is currently aspirational, not
enforced by anything written.

### 2. AC-1/AC-2 are gameable: "refused by name with a reason" has no bar for what counts as a legitimate reason
"AC-1: Every numbered gap has a recorded disposition — closed... or refused
(with the reason)." There is no requirement that the reason be *good*, only
that one be *stated*. Combined with AC-2's routing ("any gap larger than [one
PR] is filed as its own task, and the filing is the disposition"), the
literal acceptance criteria are satisfiable by refusing or re-filing every
single gap with minimal-effort reasons ("out of scope for this task," "filed
as #NNN") and closing zero of them — which technically satisfies AC-1–AC-4
while defeating the actual outcome stated in the issue's own "Outcome"
section ("The gap list stops being a list… nothing is left unaddressed and
nothing is quietly dropped"). A silent-omission problem has been swapped for
a rubber-stamp problem: mass-refusal is not silent, but it is just as capable
of shipping zero closed gaps as the state the task exists to prevent. AC-3
("Refusals name what a user should do instead") is the only quality gate on
a refusal, and it is checkable by a bot for presence-of-text, not by anyone
for correctness or good faith.
**Recommendation:** add a criterion bounding how many/what proportion of
gaps may be refused-and-filed vs. actually closed, or require refusal
reasons to be reviewed against a rubric (e.g., "refusal is valid only if the
gap requires >1 PR, touches file format compatibility, or requires a design
decision reserved to the maintainer") so refusal is not simply the path of
least resistance for every gap.

### 3. "Fits one pull request" is not a defined, checkable unit
AC-2's sizing test — "the ones whose change fits one pull request" — is
inherently subjective before-the-fact (the task itself admits this: "cannot
be sized precisely before TASK-C566-1 lands"). Nothing in the issue or in
`CONTRIBUTING.md`/`ARCHITECTURE.md` defines a PR-size budget (lines changed,
files touched, review-hours) the way, e.g., `band_mw` is used elsewhere in
this same tracker's YAML front matter (`band_mw: "1-2"` appears in #658's own
header, suggesting the project *does* have a size vocabulary — "mw" bands —
that AC-2 could have used instead of the vaguer "fits one pull request").
This makes the disposition boundary between "closed here" and "filed as its
own task" an after-the-fact rationalization tool: a contributor motivated to
avoid doing the work can always retroactively decide a gap "didn't fit."
**Recommendation:** either cite the `band_mw` unit already used in this
tracker as the sizing yardstick for AC-2, or state an explicit proxy (e.g.
diff size, element/dialog count touched).

### 4. AC-4 creates a two-document synchronization hazard with no enforcement
"AC-4: The dispositions are written back into TASK-C566-1's document, so one
artifact carries both the gap and its outcome." This requires editing
#657's deliverable (a `docs/` file, per #657 AC-4) as a side effect of
closing #658, but #658 itself is the tracked unit of work/review (one PR).
There's no stated mechanism ensuring the write-back actually lands in the
same PR, stays in sync if #657's document is later revised (e.g. if a gap
numbering shifts), or is reviewed with the same rigor as the code changes.
Nothing pins document-gap-numbers to code changes (e.g. a gap ID referenced
in a commit message or test name) the way #659 pins fixtures to gaps ("Every
gap TASK-C566-2 recorded as closed has a fixture here, or a recorded reason
why it is not trace-observable" — that's #659's job, not #658's, but #658 is
the one place a stable gap-ID scheme would need to originate). If TASK-C566-1
numbers gaps 1..N and #658 closes gap 7 but slightly rewords or renumbers
during write-back, #659 and #660 (both of which key off "gap" identity) lose
their anchor. **Recommendation:** require gap IDs to be immutable strings
(not just ordinal position) fixed by #657 and referenced verbatim by #658,
#659, #660.

### 5. Scope-creep risk baked into the mechanism itself: this task is a laundering device for arbitrary GUI/editor changes
"closed by a change to the state-machine element or its editor" is an open
license — `StateMachine.java` (813 lines), `StateMachineDialog.java` (1929
lines, the largest single file touched by this whole feature area) and
`StateMachineRenderer.java` (290 lines) are all fair game, and the trigger
for what counts as "closeable" is entirely downstream of a not-yet-written
document. A single task with `tier:task` / `band_mw: "1-2"` labeling can, by
its own acceptance criteria, greenlight edits across all three files
simultaneously as long as each individual gap is deemed "one PR's worth."
Given the codebase's own architecture notes register adding
element-behavior changes as touching "roughly sixteen places" (see
`ARCHITECTURE.md`, "Adding an element today"), even a handful of closed gaps
touching output-mode semantics (e.g. adding Mealy support — see Finding 6)
could legitimately cascade through save/load, `Attribute` entries, copy,
rotate, help topics, and `AllElementsRoundTripTest`/`HelpTopicsTest`
fixtures — all inside what's nominally "one task."
**Recommendation:** state explicitly that gaps requiring changes to the save
format, the sixteen-place element-addition checklist, or existing golden
tests are automatically routed to AC-2's "own task" branch regardless of
apparent PR size.

### 6. The issue is silent on backward compatibility for the one class of gap most likely to appear: output-mode semantics
Grepping the state-machine implementation shows no `Mealy`/`Moore`
vocabulary in `src/jls/elem/StateMachine.java` at all — those terms appear
only in the HDL exporter (`src/jls/hdl/HdlModel.java:741-790`, a `MooreOutput`
record with a comment "Moore outputs are a issue-#98 S5 rule... hold its
state"). That's strong circumstantial evidence the shipped element is
Moore-only (outputs bound to state, not to state+input), which is exactly
the kind of "named gap" #657 is likely to surface, referencing Mealy support
by name in #566's own body ("Moore and Mealy output modes"). Yet #658 says
nothing about `Circuit.FORMAT_VERSION` / save-format compatibility
consequences if closing that gap requires a new persisted attribute on
existing `.jls` files containing state machines (`ARCHITECTURE.md`'s save/load
section: `FORMAT_VERSION` header, `SaveTags`, per-element `setValue`
protocol). A gap this central to the feature's premise is also the one most
likely to break old-file loading if handled carelessly, and the issue's
acceptance criteria never mention format compatibility, `FormatHeaderTest`,
or `SaveTagsTest` — tests the architecture doc explicitly says any new
element surface must satisfy.
**Recommendation:** add an acceptance criterion (or a boundary note) that
any gap closure touching persisted state-machine attributes must state its
`FORMAT_VERSION`/back-compat story, and must pass (or extend)
`FormatHeaderTest`/`AllElementsRoundTripTest`.

### 7. Boundary notes correctly defer testing and headless work — solid, one line
"Tests for the closed gaps are TASK-C566-3; headless callability is
TASK-C566-4" cleanly matches what #659 and #660 actually claim to do (I
fetched both — #659 requires "every gap TASK-C566-2 recorded as closed has a
fixture," #660 requires headless surfaces per #657's assessment). This
separation of concerns is coherent and not gameable in the way findings 1-6
are. No further comment needed.

### 8. CAP-31's kill criterion (KC-31-2) is not surfaced in #658, creating an implicit but unstated conflict
CAP-31 (#515) states: "KC-31-2: If PF-4's assessment shows the existing
state-machine element already at parity, PF-4 closes as verified with the
document as the artifact." #657 (TASK-C566-1) repeats this verified-close
path explicitly. #658 does not mention it at all — it is written as if
disposing of gaps (plural, assumed to exist) is inevitable. If #657's
assessment finds zero gaps (full parity), #658 has no defined
behavior — is it then closed as not-applicable, or does its "no item leaves
the assessment without a disposition" framing force it to stay open pending
a gap list that will never be nonempty? A minor gap, but worth naming since
every sibling task in this chain inherits the same ambiguity.
**Recommendation:** add a boundary note mirroring #657's and #660's
explicit "if the assessment shows parity, this task closes as verified /
not-applicable" language.

## Verdict

**needs-rework.** The task is blocked on a nonexistent input (acceptable,
by design) but layers on top of that an unenforceable sizing claim (Finding
1), acceptance criteria that can be satisfied by refusing everything instead
of fixing anything (Finding 2), an undefined unit of "fits one PR" (Finding
3), a cross-document sync requirement with no anchoring mechanism (Finding
4), an open license to touch three large, architecturally-entangled files
under a single task's nominal budget (Finding 5), and no acknowledgment of
the save-format compatibility risk that its most likely gap (Mealy output
support) would trigger (Finding 6). None of these are fatal to the *idea* of
the task, but as written the acceptance criteria do not actually force the
stated outcome ("nothing is quietly dropped") — they force only that every
gap have *some* sentence attached to it, closed or not.
