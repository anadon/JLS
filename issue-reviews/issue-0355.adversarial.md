# Issue #355: FEAT-011 (RESIDUAL): a screen reader reports a circuit rather than one opaque panel, and a first run offers a next move — keyboard operability itself already ships
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

This is a well-structured planning issue (a "residual" feature spec, not an implementation ticket) with unusually strong self-verification discipline: nearly every claim cites a file, a line range, or a `git grep` command. Spot-checking the in-tree claims against HEAD (`3b6d6ec`) confirms they hold today: `Theme.java`'s `DEFAULT`/`CLASSIC` pair (lines 57-79), the dark-variant rationale comment (lines 27-31), `UserPrefs.java`'s four keys (lines 32-38), `SimpleEditor.java`'s focus-follows-mouse removal (lines 1294, 2576-2577) and the two `setAccessibleName` call sites (2412-2413), the 35-entry `ElementRegistry.ALL` list, and the flatlaf-evaluation doc's unrun-scaling-matrix admission all match. The dependency graph against live GitHub state (`#316` open and blocking, `#380`/`#381` filed and open) is also accurate as of today. The issues below are about what the plan asserts beyond that verified surface, not about fabrication.

## Findings, most severe first

### 1. (High) The issue's own body is already partly obsolete as of today's comment, and the stale half was not struck from the body text

Comment 3 (posted 2026-08-08, same day as this review), titled "ROSTER AND CAPABILITY STATEMENT NARROWED," states plainly: *"This feature's capability statement has two halves… The second half ['a first run offers a next move'] is now owned by the CAP-27 cluster and should be read as struck from this feature… goes to #550 / #511 [and] #548."* Yet the issue **title**, **Abstract** ("A first-time user… After this feature, opening JLS offers a new circuit…"), **Intended Audience & Impact** bullet 3, **Capability Statement** bullet 5, **§5 integration criterion 3** ("A first run end to end"), and the **Completion Criteria** checklist are all still written as if onboarding is this issue's scope. A contributor who reads the body and stops at the machine block (as the `blocked_by`/`blocks` DAG note tells readers to) will build the welcome-state/samples work a second time against #550/#548/#511's now-more-specific specs (#770/#771/#764/#766 pin exact test names and a ten-circuit minimum this issue never mentions). Comment 2 already flags the general shape of this hazard ("the hazard is double-filing… already live") for #73's residual; comment 3 shows it has now happened to #355 itself, mid-review.

**Recommendation:** edit the issue body (title included — half its title is the struck half) to remove the onboarding capability statement and re-point at #550/#511/#548, or add a REPLAN comment that is unambiguous about which of §1/§5/Completion-Criteria bullets are dead. Do not rely on a reader finding comment 3.

### 2. (Medium) Totality criterion is satisfiable in a way that defeats its own purpose

§5 criterion 1 requires every registered element type to yield "an accessible child with a non-null name and a role, asserted by a test that walks the registry." The transformation formula in §3 requires only `name(e)`, `role(e)`, `bounds(e)`, and selection — with **no stated requirement that the name or role be semantically distinct per element type**. A build that maps all 35 types to `AccessibleRole.PANEL` and a name of `type + " element"` (e.g., every gate reads "AndGate element", "OrGate element") satisfies "non-null name and role... a thirty-sixth type fails the build until it is mapped" literally, while still leaving a screen reader user unable to distinguish an AND gate from a state machine by role, or two same-typed elements without reading coordinates. The Abstract's own success bar — *"the tool is operable rather than merely reachable"* — is not what the stated test actually checks. Nothing in §5 asserts role-vocabulary richness (e.g., that gate types map to distinct `AccessibleRole`s where Swing's enum permits it, or that `name(e)` includes the instance name rather than only the type).

**Recommendation:** add an explicit criterion that role/name carry per-type semantic content (e.g., assert against a golden name/role table keyed by element type, not just non-nullness), so a build can't pass by mapping every element to the same generic child.

### 3. (Medium) The modal-inertness audit is explicitly not a regression test

Capability Statement bullet 2: *"Every window-scoped key binding is either proven inert while a modal dialog is up, or documented as deliberately live, **from an audit rather than an assertion**."* This is the one piece of the accessibility surface in this issue that is deliberately exempted from the "checklist stays executable, each row has a red-making mutation" invariant (§4.1) that governs everything else here. An audit performed once at close-out gives no protection against a later PR adding a new window-scoped binding that leaks through an open modal — exactly the kind of regression the rest of this issue is designed to prevent (see the #37 dead-popup-code precedent cited in #316, which this codebase has already been bitten by once).

**Recommendation:** either convert the audit into an enumerable, re-runnable assertion (e.g., a test that walks all registered `KeyStroke` bindings and asserts each is disabled or on an explicit allow-list while a modal `JDialog` is showing), or explicitly waive it under global invariant 1's "stays executable" bar and record why a per-item exception is acceptable here.

### 4. (Medium) Cost gap has no forcing function and Open Question 4 defers it indefinitely

The cost reconciliation states the named task rows (4 wk) are 1.5x-2.5x under the registry band (6-10 mw), attributes the gap correctly (35-type role mapping + three-platform scaling matrix), and then says explicitly: *"Which number a scheduler uses is Open Question 4… Does not block filing children."* Since it doesn't block filing, and TASK-0029/TASK-0030 are already filed as #380/#381 at the smaller 2-wk-each estimate, the gap is structurally likely to be discovered mid-implementation rather than resolved up front — the failure mode this document's own machinery (REPLAN, cost bands) exists to avoid elsewhere.

**Recommendation:** either commit to the 4 wk figure as the working estimate (and say why the band is wrong) or block TASK-0029/0030 acceptance on re-costing before implementation starts, rather than leaving both numbers live with no owner.

### 5. (Medium) Hidden assumption: the accessible-scene-model boundary is silent on nested SubCircuits

§3's transformation totality contract ("every element type yields exactly one accessible child, wires as relations not children") is stated as bounding the model's size. But `SubCircuit` is one of the 35 registered types (`ElementRegistry.java`), and a subcircuit instance visually and functionally contains an entire nested circuit. The issue never states whether the accessible tree recurses into a `SubCircuit`'s internal elements (in which case the "bounded, proportional to design content" claim in Open Question 1 is false for hierarchical designs — a single subcircuit reference can pull in an arbitrarily large nested graph) or stops at the SubCircuit as one opaque accessible child (in which case the tool has simply moved the "one opaque panel" problem this issue exists to fix down one level of nesting, for exactly the kind of design — hierarchical, reusable blocks — that a course is most likely to assign).

**Recommendation:** Open Question 1 should explicitly cover the SubCircuit case, not just "drawn elements plus selection" in the abstract; recommended-default (a) needs a sentence on hierarchy before TASK-0029 is scheduled.

### 6. (Low) Phantom evidence commit — cannot be independently verified in this checkout

`evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` does not resolve here: `git cat-file -e 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` fails, and it does not appear in `git log --all` (267-commit shallow clone, HEAD `3b6d6ec`). This matches a pattern several sibling reviews in this same fleet pass have independently flagged for other issues citing the same hash, so it is likely a shallow-clone/fetch-depth artifact rather than a fabrication specific to this issue — and every in-tree claim I could re-check against current HEAD (Theme.java, UserPrefs.java, SimpleEditor.java, ElementRegistry.java, the a11y checklist, the flatlaf doc) matched the quoted text exactly, which is evidence the citations are honest even if the pinned commit itself isn't locally fetchable. Flagging as low severity given that corroboration, but a reviewer with only this checkout cannot independently confirm the commit boundary the whole "shipped vs. residual" argument rests on.

**Recommendation:** if this hash is meant to be authoritative, ensure it's reachable from a ref the review tooling actually fetches (tag it, or note the required fetch depth) rather than relying on incidental content matches.

## What's solid

- The task split by verification substrate (headless/synthetic-event for TASK-0029 vs. pixel/display-matrix for TASK-0030) rather than by originating issue is well-reasoned and the rejected alternatives are argued, not just asserted.
- Global invariants 2-3 (CLASSIC stays byte-identical; ThemeTest's contrast assertion must cover every shipped variant) are concrete, testable, and correctly prevent the obvious regression of the dark-variant work breaking colorblind-safety guarantees.
- The `blocked_by: [316]` edge and its rationale (accessibility assertions need #316's harness/coverage floor to avoid decaying) is honest and consistent with #316's own body, which independently lists `blocks: [..., 355, ...]` — the mirrored edge is real, not asserted only on one side.
- Scope boundaries (§1 "Explicitly Background" / "Out of scope, with the owner") are concrete and correctly point at other filed issues (#286, #287, #288, #162/#91) rather than silently absorbing adjacent work.

## Verdict rationale

The planning discipline and in-tree verifiability are genuinely strong, which is why this isn't "needs-rework." But finding 1 is a real, live problem today — the issue's own tracker has half-obsoleted its body without editing the body — and findings 2-3 mean the stated verification for the surviving accessibility half could pass while leaving the actual "operable rather than merely reachable" goal unmet. **sound-with-concerns**: proceed on TASK-0029 (#380), but only after the body is reconciled with comment 3, and only with the totality/audit criteria tightened per findings 2-3.
