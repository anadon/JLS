# Issue #739: TASK-C544-2: the circuit is navigable as an element graph — every element and connection reachable by keyboard with a spoken name, role and connection context
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is short and its two boundary claims (extends #75, extends #380, re-owns neither) are correct and well drawn. But it is not schedulable as written: it depends on an accessible model (#380) that does not exist in the tree, and the machine block's own `ordering_after: [TASK-C544-1]` omits that dependency entirely. This is not a new observation — a same-repo comment posted the same day already flags it — but the issue body itself was never corrected, so anyone reading the issue without also reading its comment thread will misjudge it as immediately actionable. I independently re-verified every code claim in that comment against current HEAD and it still holds. Beyond the ordering defect, the acceptance criteria have real gameability gaps: AC-2's "derived from the accessible model" is not tied to any concrete interface, and AC-1's "explicit enumeration path" for unconnected elements is unspecified.

## Findings, most severe first

### 1. [CRITICAL] The stated prerequisite is not the real prerequisite, and the real one does not exist yet

Body: `ordering_after: [TASK-C544-1]` (i.e. #737 only). But AC-2 requires every traversal stop's name/role/connection-context to be "derived from the accessible model rather than from a second description mechanism," and the Outcome section credits that model to "the capability #355's static accessible-child reporting (task #380)." #380 is open, and I confirmed independently (not just trusting the in-thread comment) that its deliverable is entirely absent at current HEAD `db5ddc86` (2026-08-08, i.e. *after* the comment was posted):

```
$ git grep -n 'public AccessibleContext getAccessibleContext' -- src/ ; echo exit=$?
exit=1
$ git grep -rn 'AccessibleRelation|getAccessibleChildrenCount|getAccessibleChild\(' -- src/ test/ ; echo exit=$?
exit=1
```

`src/jls/edit/SimpleEditor.java:1121` — `private class EditWindow extends JPanel implements ActionListener,...` — overrides nothing accessible. There is no accessible model to derive a traversal stop's name/role/connection-context from, and no relation set to walk. #380 itself carries `blocked_by: [316]`, and #316 (FEAT-008, the UI harness / `jls.edit` coverage floor) is also open with its residual entirely unstarted (`SimpleEditor.java` still 5,852 lines, `enum State` still inlined, no `jls.edit` JaCoCo rule in `pom.xml`). So the real chain is #316 → #380 → #739, and none of those three links exists.

An executor who picks up #739 today, following only its stated `ordering_after`, has no way to discover this from the issue body alone — only from a comment. The machine block is the part of these issues meant to be machine-readable and load-bearing; leaving it stale while the correction lives only in a comment is itself the defect, independent of whether the correction is right (it is).

**Recommendation:** Edit the issue body's `ordering_after` to `[TASK-C544-1, 380]` (as the comment already proposes) rather than leaving the fix as an unincorporated comment. Do not schedule #739 until #380 has landed a queryable accessible model.

### 2. [HIGH] The feature-level funding gate (#737) is also open, unresolved, and itself sits behind an unrelated feature

`ordering_after: [TASK-C544-1]` correctly names #737 as a prerequisite, and #737 is real — but it is also unresolved. Its own body states it "remains the kill gate — if Swing cannot deliver a live announcement at all, this task should not be funded." I traced #737's own `ordering_after: [TASK-C549-1]` and confirmed by search that TASK-C549-1 is #756 ("a keyboard-unreachable dialog fails the build"), an open task from a *different* feature (#549) with no visible relationship to #544's screen-reader work other than sharing the accessibility harness generally. So the full unresolved chain gating #739 is: #756 → #737 → #739, in parallel with #316 → #380 → #739 (finding 1) — four open issues, zero landed, before #739 can start with a straight face.

The band declared for #739 itself (`band_mw: 2-2.5`) says nothing about this. Read in isolation the issue looks like a small, close-to-ready task; read against its real dependency graph it is behind at least two separate unresolved feature-level questions (whether Swing can announce live state at all, and how far #380's accessible model reaches — see #355's Open Question 1, still unresolved as "(a) drawn elements + selection" is only a *recommended default*, not a decision).

**Recommendation:** Do not treat the 2-2.5 mw band as a scheduling signal on its own. State plainly in the issue (not just inferable from four other issues) that #739 cannot start before #737's verdict and #380's landing, and that #737's verdict itself depends on #756.

### 3. [MEDIUM] AC-2's "derived from the accessible model" is not tied to a concrete interface, so it is gameable

AC-2: each stop must be "derived from the accessible model rather than from a second description mechanism." This is a directional constraint with no testable boundary. #380's own §7.4 defines a specific public interface (`getAccessibleContext()` returning an `AccessibleJComponent` subclass, with `AccessibleRelation` between wire endpoints). #739 never cites that interface by name — it just says "the accessible model," informally. That gap is exploitable in both directions:

- An implementation could build a *second*, parallel graph-walking data structure that happens to be populated from the same source elements #380 uses, technically deriving from "the model" in a loose sense while not actually querying #380's `AccessibleContext`/`AccessibleRelation` objects — which is exactly the "second description mechanism" AC-2 says it forbids, and nothing in the acceptance text would catch it.
- Conversely, once #380 is built, if its Open Question 1 (scene-model reach: (a) elements-only vs. (b) wire segments vs. (c) full geometry) resolves to (b) or (c), #739's traversal alphabet changes silently, and AC-1/AC-2 as worded give no signal that they need re-reading — a point the same-day comment on #739 raises but which the acceptance criteria themselves don't guard against.

**Recommendation:** Tighten AC-2 to name #380's concrete interface (`AccessibleContext`/`AccessibleChild`/`AccessibleRelation`) as the required data source, not "the accessible model" in the abstract, so a reviewer can mechanically check that traversal calls through those types rather than a lookalike structure.

### 4. [MEDIUM] AC-1's "explicit enumeration path" for unconnected elements is unspecified, and AC-3's headless assertion invites a narrow fixture

AC-1: "an unconnected element is reachable by an explicit enumeration path" — no ordering is specified (paint order? insertion order? stable-id order, matching #380's own ordering contract in its §7.10?). If the enumeration order diverges from #380's stable-id ordering, the "graph traversal" and "enumeration" paths could disagree about what order elements appear in, which is a user-facing inconsistency the acceptance criteria don't rule out.

Separately, AC-3 only requires that traversal "is asserted headlessly against the accessible model" — it does not require coverage over multi-fanout wires (one output driving several inputs), cycles/feedback loops (a valid and common circuit shape — flip-flops, latches), or buses. The example text in AC-2 ("output of AND gate 3, driving input B of the adder") is singular-fanout and acyclic; a minimal test fixture built only to satisfy AC-3 literally could pass while never exercising the connection-context phrasing for a gate whose output drives three different inputs, or a traversal loop-termination policy for a circuit with feedback — both of which are real JLS circuit shapes (flip-flops appear in the element registry) and both of which are exactly the kind of edge case that makes "connection context" hard to get right.

**Recommendation:** Specify the enumeration order for AC-1 explicitly (recommend: same stable-id order #380 uses, for consistency). Add an explicit acceptance requirement that the headless assertion in AC-3 covers at least one multi-fanout wire and one feedback cycle, with a stated traversal policy for cycles (e.g., visited-set termination) — otherwise "every element and connection reachable" (AC-1) is unfalsifiable for exactly the circuits where traversal design is hardest.

### 5. [LOW] Solid, no action needed

- The boundary statement — "This extends the shipped keyboard operability of #75 and the accessible children of #380; it does not re-own either" — is accurate against both cited issues' own scope sections and does not create the kind of scope-duplication #355/#380 explicitly warn against.
- AC-4 (reuse #75's shared `Action` layer, no new focus model) is concrete and verifiable: `src/jls/edit/EditOp.java:32` (`public enum EditOp`) is the real shared layer #75 shipped, so this constraint has a genuine target to check against, not an aspirational one.
- `part_of_feature: 544` and #544's own text ("does not re-own" §355/#380) are mutually consistent — no contradiction between the task and its parent feature on scope.

## Verdict rationale

The issue is not internally incoherent and its scope boundary is well drawn, but it is not currently workable: its own machine block understates its dependencies (finding 1), the funding gate it does cite is itself blocked by an unrelated feature four hops away (finding 2), and two of its four acceptance criteria have real gaps a minimal implementation could exploit to pass without delivering the stated outcome (findings 3-4). `needs-rework`: the ordering block and AC-2/AC-1 wording need a REPLAN-style correction before this is safe to schedule, even though no work should start regardless until #380 and #737 resolve.
