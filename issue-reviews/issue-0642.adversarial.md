# Issue #642: TASK-C563-2: above the stated input count the tool shows the 2^N arithmetic and refuses, and a selection with feedback names the offending element
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

Task-tier issue under #563 (FEAT-C31-1). Two refusal paths for truth-table
extraction: (1) above a stated input-count bound, refuse fast and show the
`2^N` row-count arithmetic instead of hanging; (2) a selection containing
sequential elements or feedback is refused with a diagnostic naming the
offending element, worded distinctly from (1). AC-4 explicitly forbids
building a second feedback-detection pass here — the diagnosis must come
from "#306's extractor."

## Findings, most severe first

**1. [Critical] AC-4's dependency ("#306's extractor") does not exist, and
even the issue filed to fix that doesn't repoint #642.** Quoted: "AC-4: The
rejection reason comes from #306's extractor rather than a second feedback
analysis written here." I grepped `src/` for any combinational-cone /
sequential-rejection component (`combinational.*cone`, `extractor`,
`SubgraphExtract`, case-insensitive) and found nothing except the
general-purpose `HdlExporter` (`src/jls/hdl/HdlExporter.java:422-437`),
which is a *full-circuit* Verilog emitter, not a combinational-only cone
extractor — its `EXPORTED` set at `:426-428` explicitly *includes*
`Register.class`, `Clock.class`, `StateMachine.class`, `ShiftRegister.class`
(sequential elements it emits, not rejects); only `Memory`, `SubCircuit`,
`RegisterFile`, `FieldExtend` are refused (`:431` `REJECTED`... actually
`:461-477` per #306's own body). #306 (CAP-09) is itself an open capstone
whose `requires_features` are `[317, 322, 335, 347, 353, 354, 359, 369]` —
I fetched #317 directly and it is CI timeout/lane infrastructure, unrelated
to circuit extraction. None of the eight is titled or scoped as a
sequential/feedback-rejecting cone extractor. This exact gap was already
diagnosed by issue #872 (TASK-C563-0, filed to self-supply the missing
component) — and that issue's own review (already on disk at
`/home/user/JLS/issue-reviews/issue-0872.adversarial.md`, finding 4) states
that #872's completion criteria never require updating #641/#642/#655's
`ordering_after`/AC text to point at #872 instead of #306. So today, and
even after #872 lands as filed, #642's AC-4 keeps citing a component (#306)
that neither exists nor is scoped to deliver this. **Recommendation:**
repoint AC-4 at the actual owning issue (#872, or whichever successor
lands), and until that component exists, mark #642 blocked rather than
`tier:task` (which reads as near-term, bounded work).

**2. [High] AC-3's refusal depends on a "sequential" classification that is
itself contested and, per the project's own docs, currently wrong in the
one place it has been drafted.** #872's review (finding 1, independently
verified against `docs/simulation-semantics.md` §6.3 and
`src/jls/elem/ShiftRegister.java:21-25`) shows the self-filed extractor's
seed classification wrongly lists `ShiftRegister` and `DelayGate` as
state-holding, when the project's own normative doc calls `ShiftRegister`
"a Mux-style combinational element, not a clocked register" and the source
comment says "despite the name it holds no state." If #642 refuses
selections using whatever classification #306/#872 eventually ships, and
that classification repeats this error, #642 will refuse valid
all-combinational selections containing a `ShiftRegister` or `DelayGate` —
directly undermining #563's own AC-1 (a correct table for real
combinational circuits) and shrinking CAP-31's demo slice. #642 states no
AC requiring a negative test (a selection containing `ShiftRegister` or
`DelayGate` must NOT be refused) to catch this inherited defect before it
ships. **Recommendation:** add an AC pinning at least one non-obvious
combinational element (ShiftRegister, DelayGate) as a must-not-refuse case,
cross-checked against `docs/simulation-semantics.md` §8's actual sequential
list rather than an ad hoc "holds state" heuristic.

**3. [Medium] The issue's own boundary note contradicts the coordinating
comment posted on the same issue.** #642's boundary note: "The bound's
numeric value is a stated decision recorded with this task; #564 and #565
must use the same number or state why theirs differs" — this *is* a
reconciliation-obligation sentence. But the coordinating comment (same
issue, same day) states: "#649's own boundary note says ... 'must be
reconciled with #563's extraction bound and #565's table-entry bound.' #642
and #652 carry no matching sentence. That asymmetry is the live risk in
this trio." The comment's summary is directly contradicted by the body it
is commenting on. Whoever reconciles #564/#565's bound values against this
comment's account will wrongly conclude #642 imposes no such obligation,
when its own text says the opposite. **Recommendation:** correct the
comment (or the body, whichever is stale) so the two agree; this is exactly
the kind of drift the comment says it exists to catch.

**4. [Medium] AC-2's "fixed time" is unquantified, and the arithmetic
display itself has an unaddressed overflow risk.** "a test at bound+1
refuses within a fixed time" names no number — 1 second and 30 seconds are
both "fixed," so a compliant-but-slow implementation (one that still does
partial enumeration work before its bound check fires) satisfies the AC
literally while violating the Outcome's actual promise ("never hangs...
rather than watching a spinner"). Separately, nothing states what
arithmetic type displays "2^N rows": a naive `1L << N` silently gives wrong
output for N ≥ 64, and even a modest N in the 60s already produces a row
count no test will ever exhaustively enumerate to verify against — the
issue never says whether the displayed arithmetic is computed lazily (safe)
or by actually attempting any part of the 2^N enumeration (unsafe, and the
one thing AC-2 exists to forbid). **Recommendation:** state the time bound
numerically and require the row-count arithmetic to be computed without
enumeration (e.g., `BigInteger` or a symbolic "2^N" display), not derived
from a partial run.

**5. [Medium] The bound's actual numeric value is asserted to be "recorded
with this task" but is nowhere in the issue.** AC-1 requires the bound be
"visible in the UI," and the boundary note calls it "a stated decision
recorded with this task" — but no number appears anywhere in #642's body.
Per the sibling coordinating comment, this number is not a free choice:
enumeration is `Θ(2^N)` and is a different complexity class from #649's
minimization bound and #652's hand-entry bound, so "the same task that
states the number" carries real weight for #564/#565 to inherit or
diverge from. As written, an implementation could ship any bound (or a
placeholder like "TBD, see config") and technically satisfy AC-1's literal
text while leaving the actual cross-issue commitment unmet.
**Recommendation:** either put the number in this issue now, or make the
boundary note's phrasing conditional ("the implementer records the number
here at land-time") rather than implying it is already decided.

**6. [Low] "distinct in wording" (AC-3) is satisfiable by any two
byte-different strings, not necessarily two strings a student can tell
apart by cause.** The Outcome's own framing is pedagogical ("a student
learns why the tool said no"), but the AC's bar ("distinct in wording") is
met by, e.g., two internal error codes with no explanation of what changed.
**Recommendation:** require the two messages to name their distinct causes
(size vs. specific element), not merely differ textually.

## What's solid

- AC-1's citation to CAP-31 AC-3 is accurate: fetched #515 directly, and its
  AC-3 text ("Bounds are stated and enforced: above N inputs the tool
  refuses with the arithmetic, never hangs") matches what #642 claims to
  implement.
- The size-refusal vs. feedback-refusal split (AC-1/AC-2 vs. AC-3) is a
  clean, non-overlapping scope boundary and is consistent with #563's own
  AC-2/AC-3 split.
- Deferring the batch/headless surface to a separate task (per #641's
  boundary notes, TASK-C563-4) rather than folding it into this GUI-facing
  task is correct scope hygiene.
- AC-2's "never starts and gets cancelled" requirement is a genuinely
  testable, non-vague acceptance bar for the core no-hang promise, modulo
  finding 4's missing time bound.

## Verdict rationale

`needs-rework`: the task's own scope split (size refusal vs. named-element
refusal) is sound, but its central mechanism — AC-4's reuse of "#306's
extractor" — points at a component that does not exist in the codebase,
is not actually scoped by #306's required-feature set, and is not repointed
by the one issue (#872) filed specifically to supply it. That is not a
nitpick: it is the difference between "reuse an existing diagnostic" (what
AC-4 mandates) and "write the sequential/feedback detection here" (what
AC-4 forbids) — and as things stand, an implementer has no real component to
reuse. Compounding that, the classification such a component would need
(what counts as "sequential") is independently shown, in #872's own review,
to be wrong for at least two common combinational elements, which #642
inherits without any negative test to catch it. The numeric bound this task
is supposed to fix for two sibling issues is asserted as decided but never
stated, and the issue's own coordinating comment misdescribes the issue's
own boundary note. None of this is a concept problem — refuse-fast with
shown arithmetic, and a separately-worded named-element refusal, are both
reasonable — but the issue cannot be picked up as a bounded 1-week task
until the extractor dependency is repointed to something real and the bound
number is actually recorded.
