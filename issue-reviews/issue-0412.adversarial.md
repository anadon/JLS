# Issue #412: TASK-0038: a program builds a circuit by naming verbs, not by emitting save text, and a mistyped attribute is a diagnostic instead of a silent drop
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is unusually well-evidenced for its factual claims about the current tree (file counts, line citations, and structural observations were spot-checked against HEAD `e7731bd` and matched almost exactly), and its falsification criteria are genuinely falsifiable. But its central selling proposition — "no verb writes save text and no verb reparses one" — is contradicted by the very mechanism (H1, §7.10) it proposes to build the verbs over, and one of its two "blocks execution" open questions is answered one way in the Open Questions section and a different way in the operative Method/Materials sections of the same document. Both are load-bearing enough to send back for rework before filing a sub-task or starting implementation.

## Findings, most severe first

### 1. [HIGH] The headline contract — "no verb writes save text and no verb reparses one" — is false for the proposed design

The Abstract states: *"every verb constructs a `CircuitOp` and submits it through `OpSink`, no verb writes save text, and no verb reparses one."* §7.4 repeats it as the **Contract**. But §7.10's own formal definition of `place` composes into `AddElements`:

> `place(t, m) = ... id(OpSink.submit(AddElements([ρ(t,m)])))`
> where ρ renders: `"ELEMENT " ⌢ t ⌢ "\n" ⌢ (attribute lines) ⌢ "END\n"`

That is a save-format `ELEMENT ... END` block, character for character. `AddElements` is a shipped op (`src/jls/collab/op/AddElements.java:37`, `public record AddElements(List<String> blocks)`), and its `apply` calls `ElementBlocks.load(circuit, block)` (confirmed at `AddElements.java:57-58`) — i.e. it parses that rendered text back into an `Element`. The project's own `docs/operation-layer.md:52` documents this explicitly: a block is escaped save-format text that "loads such a block back through the [loader]." §7.5 half-admits this ("The block renderer that turns a typed attribute map into an `ELEMENT` block... must not become API: it is the emit-text idiom in miniature") but the Abstract and §7.4 still assert the opposite in plain English.

This matters beyond wording: O4's own root-cause diagnosis is that `Element.setValue`'s attribute loop silently drops unknown names when reparsing rendered text (`Element.java:344/359/374/389`, confirmed present). The fix P2 proposes is a validation guard placed *in front of* that exact reparse path — not a structural elimination of render-then-reparse. That's a defensible engineering choice, but the issue should say so, not claim the pattern it exists to eliminate (O2) has been eliminated when it has only been pushed one layer down and relabeled private.

**Recommendation:** Rewrite the Abstract/§7.4 contract to something falsifiable and true, e.g. "no verb writes save text to a file or invokes the file loader (`Circuit.load`); attribute names are validated before rendering into an `AddElements` block." Or, if the "no reparse" claim is meant literally, it requires a *new* op kind that takes typed attribute maps directly — which H1 explicitly rules out ("no new op kind is required").

### 2. [HIGH] Open Question 1 is answered two contradictory ways in the same document

Open Question 1 asks "Where does the verb set live — `jls.api` or `jls.collab.op`?" and states plainly: *"Recommended default: follow the parent feature — `jls.collab.op`... do not put the verbs in `jls.edit`"* and marks itself **"Blocks execution."** #337 (the parent feature, verified open) is even more pointed: `jls.api` "begins an extensibility story this feature does not own."

Yet the same issue's operative sections already commit to `jls.api`:
- §6 Materials & Apparatus: *"To be built: the verb class, `test/jls/api/CircuitBuilderTest.java`..."*
- §8 Method (first substantive step): *"Write `test/jls/api/CircuitBuilderTest.java` first and observe P1 and P2 red."*
- §9 Data Collection & Analysis: *"`test/jls/api/CircuitBuilderTest.java`: ..."* (repeated for six test methods)

An implementer following the checklist literally (§8 is a checkbox list meant to be executed top to bottom) creates `test/jls/api/...` before Open Question 1 is even nominally resolved, and in the direction the recommended default advises against. This is not cosmetic: `test/jls/api` does not exist in the tree today (confirmed), so its creation is itself a decision with the exact consequence (an unowned `jls.api` extensibility surface) the recommended default was trying to avoid.

**Recommendation:** Either resolve Open Question 1 to `jls.api` and update the recommended default and #337's cross-reference accordingly, or fix §6/§8/§9 to say `test/jls/collab/op/CircuitBuilderTest.java`. Do not ship both answers in one issue.

### 3. [MEDIUM] P7's coverage criterion is gameable

> "P7 (must hold after). A reflective enumeration of the builder's public methods asserts each appears in at least one test, so a verb cannot ship untested."

"Appears in at least one test" is satisfied by a single smoke-test call with no assertion on behavior. As written, P7 can go green while `configure()` or `remove()` are exercised only for their happy path with no rejection-path coverage, no invariant checked. Given the issue's own emphasis elsewhere ("Rejection tests assert the message text... not a boolean," §9) on being precise about what a test actually proves, P7 is noticeably looser than the standard the rest of the document holds itself to.

**Recommendation:** Tighten P7 to require, per verb, at least one assertion-bearing success case and (where applicable) at least one assertion-bearing rejection case, not mere invocation.

### 4. [MEDIUM] The stated blocker likely makes this issue non-actionable for a while, and the fallback is unspecified

`blocked_by: [382]` is accurate and consistent — `CircuitOp.apply` is confirmed still typed `(Circuit, Graphics)` at `src/jls/collab/op/CircuitOp.java:51` in the current tree, and #382 (verified open) is a real, nontrivial task: 12 files lose `java.awt.Graphics`, and four inline editor gestures ("the hard half," per #382's own Threats to Validity) must migrate to ops. The Method's first bullet allows proceeding anyway via "record a waiver per rule 10," but the issue never says what the seven verb signatures would look like, or how P3's byte-parity test would be written, if `place`/`configure`/etc. still have to thread a `Graphics` (or null) through to `AddElements.apply` because #382 hasn't landed. Given O6's finding that `null` Graphics already works for the five element types the worked examples use, a waived start is plausible but the issue gives the implementer no guidance on what changes if #382 is still open when this is picked up.

**Recommendation:** Either state explicitly that this task cannot start (not just "should not") until #382 lands, or specify the degraded/waived verb signature so a partial start doesn't produce throwaway work.

### 5. [MEDIUM] A cited "other consumer" issue closed as a duplicate shortly after this issue was filed

§12 Related Work lists #326 (FEAT-038, the drawn RV32 machine) as an open "other consumer" of the verbs, and the parent feature #337's `blocks` list and mermaid graph make the same claim. As of this review, #326 is **closed, state_reason: duplicate** (closed 2026-08-04, one day after #412 was opened on 2026-08-03). The issue's single-owner bookkeeping and DoD item ("Landing reported on #337... and on #326 and #345 as the other consumers") now points at a closed issue with no visible successor recorded in #412 itself. This is a currency problem inherent to the fleet-of-cross-referencing-issues approach, not something #412's author could have predicted at filing time, but whoever executes this task needs to re-resolve where FEAT-038's scope (and its "consumer" relationship to the verb set) actually landed before treating #326 as live.

**Recommendation:** Re-verify #326's disposition (and its duplicate target, if any) before execution; update the DoD's reporting step accordingly.

### 6. [LOW] Evidence commit is unreachable in a shallow clone; a few line citations have already drifted

`git cat-file -e 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` fails in this checkout (shallow clone), so the pinned citations can't be verified against that exact commit — only against current HEAD (`e7731bd`, 2026-08-08), where the structural claims substantially hold: `ElementVocabulary`'s 34-token `ALLOWED` set, `CircuitOp.apply(Circuit, Graphics)`, `Element.savedAttributes()` being `protected`, `ElementId.pinForTesting` being package-private, the HDL golden counts (37 `.v` + 33 `.vhdl` = 70, exact match), and the 24-file `CircuitTextBuilder` consumer count (exact match) all check out. One citation has already drifted: O5 cites `Element.java#L318` for `savedAttributes()`; at HEAD it is line 316. This is expected per the issue's own rule 6 ("re-derive line numbers if HEAD has moved") but is worth flagging as a mandatory step, not a formality, for pickup.

### Parts that are solid — noted briefly

- O4's diagnosis of the silent-attribute-drop bug is precise and independently reproducible against the cited mechanism (`Element.java`'s `setValue` loops, `docs/file-format.md:220`, confirmed verbatim: "Unknown attribute names are silently ignored").
- The `connect()` net-merge composition (§7.10, P6) is a sound, testable design over the existing `AddWire`/`RemoveWire` ops.
- O6's AWT-nullability nuance is unusually careful — it corrects the corpus's own overstatement ("a program with no display cannot apply an op" is shown false for five element types) rather than repeating it.
- §11 Threats to Validity already surfaces two of the sharpest risks (protected `savedAttributes()`, package-private `pinForTesting()`) before a reviewer has to — good self-awareness, even though it doesn't resolve them.
- The falsification criteria (§10) name concrete next actions per refutation rather than "investigate further."

## Overall

The factual grounding is strong and the failure-mode/rejection design (P2, P5) is sound in isolation. But the issue is not ready to execute as written: its core "no reparse" claim conflicts with its own chosen composition mechanism, and one of its two execution-blocking open questions is silently pre-resolved in the wrong direction inside the Method section. Both should be fixed — and #382's landing state and #326's current disposition re-checked — before this is picked up.
