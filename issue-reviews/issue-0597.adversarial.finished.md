# Issue #597: FEAT-C38-1: a student picks a board, assigns pins in a dialog the board itself validates, and clicks once — the headless board flow becomes a File-menu path with no terminal
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This issue is well-scoped in intent (a GUI surface over an existing headless
path, explicitly not a re-implementation) and its substrate claims check out
against the repository. But the issue **body itself** — the text an
implementer actually works from — contains an acceptance criterion (AC-3)
that, taken literally, requires violating a recorded, tested architectural
invariant, and another (AC-1) that is not evaluable with the code that
exists today. Both defects were already identified in a review comment
posted to this same issue on 2026-08-08, with corrected AC text proposed —
but the comment was never folded into the body, and the same broken AC-3
language was copied verbatim into a child task issue (#636). An issue whose
own thread contains an unmerged self-correction is not yet safe to hand to
an implementer.

## Findings, most severe first

**1. [Critical] AC-3 as written asks for exactly the thing the codebase has an explicit, tested invariant against.**

> AC-3: "One action from the dialog runs export + constraint emission and
> **the handoff script** and reports the outcome in the GUI; no step
> requires a terminal."

Running `scripts/icestick-handoff.sh` from a Swing dialog means spawning a
subprocess from `src/`. `#359` §4 records this as a **global invariant**,
not a suggestion: *"No subprocess in `src/`. External tools are invoked
from `test/` only, through `ToolLocator`. The single self-contained offline
jar stays offline."* Verified live on this checkout:

```
$ grep -rn "ProcessBuilder\|Runtime.getRuntime().exec" src/ | wc -l
0
```

`scripts/icestick-handoff.sh` itself states the reason (issue #215 H2,
"delegate, do not reimplement") — but that ruling is about not
re-implementing synthesis/P&R/pack, not about who is allowed to invoke the
script; invoking it from `src/` is process-driving regardless of whether
the driven tool is open-source. AC-3 would have an implementer put the
project's first `ProcessBuilder` into `src/`, breaking an invariant another
open feature (#359) is explicitly committed to preserving.

This is not a novel objection: a comment already posted to #597
(2026-08-08T17:34:28Z) makes the same point and proposes a corrected AC-3
("produces every artifact... and presents the exact handoff command line...
copyable... no process is spawned from `src/`"). **That correction was never
applied to the issue body**, and the original, contradictory wording was
carried into **#636** (TASK-C597-3) AC-1 verbatim, so the defect has already
propagated to a child task rather than being caught. Recommendation: edit
the body's AC-3 before this is picked up — a comment 4 days old that nobody
merged is not a substitute for a correct spec.

**2. [High] AC-1 is not falsifiable against the code that exists.**

> AC-1: "...the board picker's entries are read from `Boards.all()`, so a
> board added by #264/#416 appears with no GUI change."

Verified: `Boards.java` has `private static final List<Board> ALL =
List.of(ICESTICK);` (one entry, no registration seam) and `Board.Format`
has exactly one constant (`PCF`). There is no second board or format to
observe "appearing," so the positive half of AC-1 cannot be evidenced by
any test written under this issue — only a weaker structural claim ("the
picker calls `Boards.all()` and holds no board list of its own") can be.
As worded, AC-1 invites exactly the gaming it should prevent: a test using
reflection to inject a synthetic board into a private `List.of` would
satisfy the letter of "add a synthetic board and observe it appear" (the
phrasing sibling task #632 AC-1 uses) without proving anything about the
real, immutable board table. Again already flagged in-thread; again not
merged into the body. Recommendation: adopt the in-thread correction —
assert structurally that the GUI holds no board/format/pin table of its
own, and treat the "new board needs no GUI change" claim as unverified
design intent until #416 actually lands a second board.

**3. [Medium] AC-2 / AC-5's "byte-identical" claim is gameable as worded.**

> AC-2: "...GUI and CLI paths emit byte-identical constraint files..."
> AC-5: "...pinned by a test that the emitted artifacts match the CLI's."

Nothing in either AC requires the test to drive the actual CLI entry point
(`JLSStart`'s `-board`/`-pins` argument handling, `JLSStart.java:387-427`).
A test that calls `PcfEmitter.emit(...)` twice with the same in-memory
arguments — labeling one call "GUI" and one "CLI" — satisfies the literal
text while proving only that the function is deterministic, which nobody
doubted. Tighten to require exercising `JLSStart`'s real argument-parsing
path for the "CLI" half, not just the shared emitter.

**4. [Medium] Feasibility/scope: the corrected AC-3 and sibling task #636's own AC-2 are in tension.**

#636 AC-2 requires: *"Progress and outcome are reported per stage... a
long-running handoff does not present as a frozen window."* Per-stage
progress reporting on an external script's execution is very hard to do
honestly without spawning and streaming that process's output from
somewhere — which reopens finding 1 rather than resolving it. If the
"no-process-in-src" correction is adopted (finding 1), #636 AC-2 needs to be
rewritten too (the GUI can report only "artifacts written, run this command"
— it cannot report per-stage progress of a process it never starts). This
isn't reconciled anywhere in #597 or #636 today, and the stated band
(`band_mw: "2-3"` at the feature level, `"0.5-1"` for #636) does not
obviously price in resolving that tension.

**5. [Low] Substrate claims are accurate and the #288 seam is real.**
`Boards.all()`, `-board`/`-pins` (`JLSStart.java:111-114`, dispatch at
`:387-427`), `PcfEmitter.emit` (`:427`), `docs/component-naming.md`, and the
`MenuAcceleratorPolicyTest`/`MenuAcceleratorFiringTest` pattern all exist as
cited. `ordering_after: [264, 288]` is correct — #288 explicitly carves out
"board/pin-constraint selection UI" as future scope (§13), and this issue is
that scope. No objection.

**6. [Low] The #598 boundary is well drawn.** The issue's own first comment
distinguishes this feature's "surface" deliverable from #598's "diagnostic
taxonomy" deliverable cleanly, and correctly assigns AC-2's rejection-text
ownership to #598. Sound; no rework needed here.

**7. [Low] KC-38-2 is a good, concrete kill criterion** ("if #264's stage 2
lands the GUI flow itself, this feature is consumed rather than rebuilt")
— clear trigger, no objection.

**8. [Low] Machine-block thinness vs. sibling issues.** Unlike #264/#359,
#597's yaml front-matter has no `blocked_by`/`blocks`/`tier` fields, only an
informal `ordering_after`. #416 is a real prerequisite for AC-1's evidence
(per finding 2) but appears only in prose ("consumed, not duplicated"), not
as a dependency edge. Minor, but this tracker otherwise leans on
machine-parseable dependency graphs, and this issue is looser than its
neighbors about it.

## Bottom line

The feature framing (thin GUI surface, not a reimplementation) is sound and
the cited substrate exists exactly as described. But two of five acceptance
criteria are broken in ways the maintainer's own review thread already
caught and proposed fixes for — fixes that were never applied to the issue
body and that have already leaked into a child task's AC text. Do not start
work from the current body; apply the in-thread corrections to AC-1 and
AC-3 first, and reconcile AC-3's correction against #636 AC-2's per-stage
progress requirement before either issue is picked up.
