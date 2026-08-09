# Issue #84: Decompose SimpleEditor — residual: extract the 9-state mouse interaction machine as GoF State objects
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The core engineering claim is sound and well-evidenced: `SimpleEditor.java` (5,852 lines at HEAD `5311625`, verified) still carries an in-file `enum State` (L770), inlined per-state branches across the mouse/keyboard handlers, three `switch (currentState)` dispatchers (L3043/L3203/L3943), and a duplicated wire-cancel sequence (L1387–L1418 vs L2801–L2836). All of the issue's §2 Observations reproduce exactly at current HEAD — `git grep` checks below confirm `enum State` at L770, `currentState` field at L1218, three `switch (currentState)` sites, one surviving `getSource() ==`, 75 `instanceof`, and 44 `setState(State.…)` call sites. That discipline is genuinely good.

But this is not the issue's body alone — it is a 5,800-word body plus **17 comments spanning a month**, several of which materially rewrite scope, contradict the body's own design, and leave a maintainer escalation open and unresolved as of the most recent comment (2026-08-08 17:50:44, same day as this review). An executor who reads only the body — which is what "pick up issue #84" normally means — will build something that a later adversarial pass will reject, or will start work that is explicitly blocked. That is a process failure independent of whether the underlying refactor is a good idea (it is).

## Findings, most severe first

### 1. [Critical] Unresolved, self-contradicting design fork — explicitly flagged as blocking execution, still open

The body's own §7.4/§7.5 specify a **GoF State decomposition**: a behavior-bearing `InteractionState` interface, nine concrete **package-private** classes ("so they do not become load-bearing API"), dispatch replaced by polymorphism, P4 requiring `grep -c "switch (currentState)"` → 0.

Comment `#issuecomment-5181489241` (2026-08-04, absorbing #441) specifies a **different, incompatible design** under the *same name*: `public enum jls.edit.InteractionState` that "carries no behaviour," plus a `public final class MouseMachine` with `Transition transition(...)` implemented as a `switch` **expression**, whose exhaustiveness is the entire point (a demonstrated compile-clean silent hole today: appending a 10th enum constant compiles with exit 0, no diagnostic).

Comment `#issuecomment-5227061320` (2026-08-08 16:44) — nine hours before the most recent comment on the issue — names the conflict explicitly:

> "The name `jls.edit.InteractionState` is claimed twice — as a behaviour-bearing interface with nine implementations, and as a behaviourless enum. Only one can exist." … "**ESCALATED — maintainer decision required before execution.**" … "**This blocks execution.**"

No follow-up comment resolves it. The three options the escalation lays out (GoF-only, machine-only, or both layers) have different costs and drop different criteria (the GoF path has no cancel-unification test; the machine path breaks P4 as literally written and makes the issue's own title false). **An executor who starts today commits to a design by accident**, exactly as the escalation warns. This alone should stop execution until a maintainer answers.

### 2. [Critical] `blocked_by` is stale in the machine-readable header, and three different comments assert three different answers

Body: `blocked_by: []` — "former same-region holds ... all landed."

Comment `#issuecomment-5181489241` (2026-08-04): asserts `blocked_by: [440]` is a **real** prerequisite — #440 (TASK-0019) sets the `jls.edit` JaCoCo floor, and `pom.xml:408-411` literally names *this issue's own work* as the precondition for the floor existing (`jls.edit is deliberately unfloored until the #91/#84 work makes editor code testable`). Verified: `pom.xml:408` still reads exactly that at HEAD. Without the floor landing first, "coverage didn't regress" (an implicit acceptance bar for any extraction moving hundreds of lines) is unmeasurable — moving code between classes moves which class JaCoCo attributes coverage to. I independently confirmed #440 is open and its own body states, in its own Status & Dependencies section, that it "blocks TASK-0020" (this issue) for exactly that reason.

Comment `#issuecomment-5227361797` (2026-08-08 17:50, the *last* comment on the issue) adds a **third, different** claim: don't start before #337 (FEAT-015, removing `Graphics` from `CircuitOp.apply`) lands, because "a state object extracted today cannot be asserted without a display, which is the entire point of extracting it." I verified #337 is open and unfinished (all 11 `apply` overrides still take `Graphics` at HEAD per #337's own body).

The body's YAML header — the field other issues' tooling actually parses (see #804/#593's `ordering_after` edges that reference this issue by number) — was never updated to reflect any of this. Three different, partially inconsistent blocking claims exist only in prose, scattered across comments, none reconciled into the one field designed to carry that information.

### 3. [High] §14 Completion Criteria is stale against what later comments call "completion criteria"

The body's own §14 checklist does not mention: the ArchUnit AWT/Swing-freedom rule that comment §K says "must land in the same commit as the machine or the property is unenforced from birth"; the `matchJump` → `JumpMatchAction` extraction and dropping `ActionListener` from `EditWindow`'s `implements` clause; the totality test over the full state×gesture product; the JaCoCo before/after comparison against #440's floor; or the `STATUS:` comment obligation to parent #316. A contributor who satisfies exactly the body's §14 will be judged incomplete by the comment thread's actual bar. This is scope-creep-by-comment with no back-reference from the enforceable checklist — the artifact meant to be the ground truth for "done" is not ground truth.

### 4. [High] An entire deliverable — ownership of the `app.command` extension-point seam — is absent from the body

`docs/extension-points.md:35` (verified in-repo) reads:

```
| Command / activation trigger | `app.command` | shim contract over `jls.module.Activation` | `jls.module` | many | register (lazy activation vocabulary) | pending (#84, with #220's runtime) |
```

and #223 (the extension-point catalog feature) is `blocked_by: [61, 62, 84, 76]` specifically because this issue is named as the owner. None of this appears in the body's Abstract, §7 Interface & Data Contract, or §8 Method — it surfaces only in the final comment, which itself says the scope is ambiguous ("(a) rides along as a small shim... or (b) #223's close-out waits on the whole of this task... (b) is the reading #223 assumed"). An executor scoping from the body has no way to know this obligation exists, let alone which of the two readings applies.

### 5. [Medium] True cost is materially larger than the body's own §6/§8 suggest

The absorbed #441 material (comment `#issuecomment-5181489241`, sections E–K) adds scope the body doesn't carry: promote the enum to a public top-level type; new `MouseMachine`/`Transition`/`Gesture`/`Context` types; an ArchUnit rule; a totality test over the full state×gesture product; a 44-call-site `invalidatesIndex` audit; the `JumpMatchAction` extraction; and nine named regression suites that only partially overlap the body's own §6 list (`DragCandidateBoundTest`, `WireSweepSymmetryTest`, `EditActionMatrixTest`, `MidPlacementPaletteFeedbackTest` appear only in the comment). Someone estimating effort from the body alone will underestimate substantially — this compounds finding #1, since the cheaper design (GoF objects) and the expensive one (machine + totality suite) are not the same amount of work, and which one is required is exactly the unresolved question.

### 6. [Medium] P4's own falsification criterion is gameable under the unresolved fork

§5 P4: `grep -c "switch (currentState)"` → `0` in `SimpleEditor.java`. This is satisfiable by literally relocating the switch into a new file (`MouseMachine.java`) under a renamed field while keeping switch-based dispatch — which is not just a hypothetical loophole, it is *literally what the competing absorbed design requires* (a `switch` **expression** is "the mechanism" for H2's exhaustiveness guarantee). As written, P4 cannot distinguish "dispatch replaced by polymorphism" from "dispatch relocated and kept," so it is not useful evidence for which design was actually built, and a reviewer checking only this grep would pass either outcome.

### 7. [Medium] A genuine bug-class exists but lives only in a comment, not in the body's own falsification criteria

Independently verified at HEAD: `setState`'s `switch (currentState)` (L3943) has no `default` arm. The comment thread demonstrated (not merely asserted) that appending a tenth enum constant compiles cleanly with `javac`, exit 0, no diagnostic — a silent behavior hole (no status message, no background color) that a warnings-as-errors build does not catch. This is real and worth fixing, but it is absent from the body's §2 Observations and §10 Falsification Criteria. Under the GoF-only design the body specifies, there is no compiler-enforced exhaustiveness mechanism at all (no switch to make exhaustive over), so this defect class has no home in the design the title actually commits to — another symptom of finding #1.

## What is solid (brief)

- Line-number citations in §2 Observations all reproduce exactly at current HEAD (verified independently via `git grep`/`wc -l`) — the issue's own "re-derive at pickup" discipline checks out today.
- The retirement of the "<1,500 lines" done-target is well-justified: the file's growth (4,119 → 5,852) while five extractions landed is a real, verifiable trend, and raw line count is a legitimately bad proxy for coupling.
- The formal H1/H2, P1–P4, §10 falsification structure is genuinely falsifiable, not vague — a rare and good property for a refactor issue.
- The duplicated wire-cancel finding (O7, L1387–L1418 vs L2801–L2836) is concrete, independently checkable, and a real defect (two divergence-prone copies of the same cancel sequence) regardless of which design wins the fork above.

## Recommendation

Do not execute against the current body. Before any code is written: (a) get the maintainer decision the 2026-08-08 16:44 comment explicitly escalates and asks for (GoF objects / behaviourless-enum machine / both layers), and edit the body's §7.4/§7.5/title to match rather than leaving three competing designs live in prose; (b) reconcile the `blocked_by` field with the two real, separately-evidenced blockers (#440's coverage floor, and the #337 sequencing claim — the latter should itself be confirmed against #316's actual dependency graph, since #316's own body only routes #337 through itself, not directly through TASK-0020); (c) fold the comment-only obligations (ArchUnit rule, `matchJump` Action, app.command seam, ninth-suite pin) into §14 so "done" is checkable from the body alone again.
