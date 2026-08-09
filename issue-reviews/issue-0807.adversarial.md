# Issue #807: TASK-C594-1: typing a part's name finds it — incremental palette search over names and aliases, with a no-match that explains itself
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The stated goal — incremental palette search closing Logisim-Evolution #1234 — is legitimate and appropriately scoped as a task under FEAT-C37-3 (#594). But the issue leans on three chains of external state (the SimpleEditor decomposition, the #592 parity catalog, and an undefined "K9" ratchet) without checking whether any of them are actually in a state that makes this task startable or verifiable, and two of its five acceptance criteria describe a data source ("the element registry") that does not currently carry the data the criteria need it to carry. None of this is fatal to the underlying feature idea, but the issue as written should not be picked up until these are resolved.

## Findings

### 1. [High] The task ignores its own capstone's hard gate, and the gate's prerequisites are not landed

#807's yaml frontmatter lists only `ordering_after: [TASK-C592-2]`. It never mentions #316 or #441, yet its own parent chain makes both hard prerequisites:

- #521 (CAP-37, the capstone owning this whole line): `ordering_after: ["#316 FEAT-008 / #84 (SimpleEditor decomposition — new editor behaviors land in decomposed collaborators, not into the god class; hard boundary)", "#441 TASK-0020 (headless interaction machine — parity fixes must be assertable without a display)"]`, and `KC-37-1: Nothing in PF-2..5 lands inside SimpleEditor — the decomposition boundary is a hard gate; if FEAT-008 stalls, this capstone waits rather than growing the god class.`
- #594 (the parent feature): "Decomposition boundary (#316 FEAT-008 / #84) — hard gate: the search index, the recently-used model and the keyboard navigation state are model-side collaborators, not fields on `SimpleEditor`. Nothing here lands in the god class; **if #316 stalls, this feature waits (KC-37-1)**."

Checked against the actual repo and both issues: #316 is open, and its own roster (updated 2026-08-08, the same day as #807) lists `TASK-0020: ... not filed` and states plainly "ABSENT at 2d0ca9d: the enum is still on the outer class ... and no `MouseMachine` or `InteractionState` type exists under `src/jls/edit/`." Issue #441 — which is literally titled "TASK-0020" — is **closed as duplicate**, not landed. `grep -rn "class MouseMachine\|class InteractionState" src/` returns nothing; `SimpleEditor.java` still carries the inline nine-state `enum State` per #316/#441's own citations. So the explicit precondition for "this feature waits" is unmet by the capstone's own bookkeeping, and #807 was filed anyway without recording the wait or even naming #316/#441 as blockers.

This matters concretely: "the search index is a model-side collaborator, not a field on `SimpleEditor`" (#807's own outcome text) is achievable as a narrow technical fact regardless of #316 (nothing stops writing a new class today), but wiring it into the actual toolbar/search UI without touching `SimpleEditor.makeElements`'s hand-rolled 5,852-line dispatch is exactly the kind of editor-surface change #592 and #594 flag as "blocked on #316 rather than scored as ready." AC-5's throwaway "(KC-37-1)" citation gives no enforcement mechanism (contrast with sibling task #804/TASK-C593-1, which ties the same claim to "the ArchUnit-style rule #441 establishes ... not waivable at task level" — a rule that, per the above, doesn't exist either).

**Recommendation:** Add `#316` (and whatever now supersedes #441) to `blocked_by`, not just an unenforced parenthetical, or explicitly record — the way #594 anticipates — that this task waits. Don't fund/start until there is a real enforcement mechanism for "outside SimpleEditor," not just a self-attested clause.

### 2. [High] AC-5's verification target does not exist yet, and its own precondition is unlanded

AC-5 requires the change to "correspond to a scored GAP row in #592's catalog." #592 (FEAT-C37-1) is open; its own outcome text says "**a reader opens `docs/` and finds** a scored inventory..." — future tense, not yet true. `find docs -iname '*catalog*' -o -iname '*ergonom*' -o -iname '*parity*'` returns nothing in this checkout. Worse, #807 orders itself after **TASK-C592-2** (#803) specifically because that is the task that adds the funding score, the acceptance-vehicle column and the stop-loss column to the catalog rows — and #803 is itself open/unimplemented. So at the moment #807 would be picked up, there is no catalog, no scored row, and no acceptance-vehicle assignment for search to "correspond to." An implementer cannot check this AC against anything in the tree; they can only assert it in prose, which is exactly the kind of criterion that lets "done" be declared without a real check.

**Recommendation:** Either block #807 on #592/#803 landing (its `ordering_after` gestures at this but doesn't actually block), or drop the "corresponds to a scored GAP row" clause from this task's AC and push it to a closing checklist that is verified against the real doc once it exists.

### 3. [High] AC-1/AC-3's alias story is contradicted by the actual registry, and is unsatisfiable as written

AC-1: "Incremental search matches element names and **at least one alias form**... over the registered element set." AC-3: "The search index is built from the element registry, so a newly registered type is findable without editing the search code."

Checked `src/jls/elem/ElementType.java` and `src/jls/elem/ElementRegistry.java`: `aliases()` is explicitly documented as a **save-file backward-compatibility mechanism** — "Historical tags that must keep resolving to this type" for renamed classes (issue #79's alias table), not a human search-synonym facility. And as registered today, **every one of the 35 entries in `ElementRegistry.ALL` (lines 38-77) is constructed with zero aliases** — `new ElementType("AndGate", AndGate.class, AndGate::new)` with no trailing varargs, for all 35 rows. So "matches ... at least one alias form ... over the registered element set" cannot be satisfied by the registry's actual alias data today: there is no alias to match against for any type. An implementer can satisfy the letter of AC-1 only by (a) fabricating new alias data that doesn't presently exist in the model (which then makes AC-3's "no code change for new types" claim false for anyone who wants their new type's alias searchable), or (b) reinterpreting "alias" to mean something AC-1 never defines (e.g. the palette's short button label "and"/"AND" vs. its tooltip "AND gate" vs. its registry tag "AndGate" — three different strings for the same type, per `src/jls/edit/Palette.java` lines 124-188).

Separately, `ElementType.java`'s own javadoc states the intended separation plainly: "GUI concerns — palette icon, category, help topic, creation dialog — belong to a separate GUI-side palette entry **and never appear here**." The human-readable name a student would actually type ("AND gate") lives only in `jls.edit.Palette`'s hand-authored `tooltip` field, not in the core `ElementRegistry`. Building the index from the registry (as AC-3 demands) gets you only opaque tags like `"AndGate"`, `"TriState"`, `"FieldExtend"`; building it from `Palette` (to get human names) contradicts AC-3's literal "built from the element registry" and reintroduces exactly the hand-maintained-list problem AC-1 says it's avoiding ("rather than a hand-maintained list") — `Palette.ENTRIES` is itself a hand-maintained `List.of(...)` table.

**Recommendation:** Name the actual field(s) the index searches (registry tag? palette tooltip? both?), and either (a) add real human-facing search aliases to the model as part of this task's scope (and say so), or (b) drop the "alias form" requirement until such data exists.

### 4. [Medium] AC-4's "K9" and "startup cost ratchet" are undefined in the repository

AC-4: "K9 holds: no new default-visible chrome, and index construction does not move the startup cost ratchet." Neither "K9" nor a "startup cost ratchet" appears anywhere in `README.md`, `ARCHITECTURE.md`, `docs/*.md`, or `pom.xml`. The only "startup" test in the tree is `test/jls/WaylandStartupCliTest.java`, which checks Wayland-toolkit CLI selection, not a cost/perf ratchet. `#521` (the capstone) uses "K9 stands" the same way, as if pre-established elsewhere, but nowhere I can reach in this repository defines what K9 is, what "moving the ratchet" means numerically, or what test would fail if it moved. As written, an implementer working only from the repo (as directed — "the search index... construction does not move the startup cost ratchet") has no way to know what to measure or what test enforces it.

**Recommendation:** Either link/define K9 and the startup-cost-ratchet mechanism explicitly (file/test name), or drop the clause from a task-level AC — a task issue should be checkable from the repo it's filed against.

### 5. [Medium] AC-2's no-match message is trivially satisfiable without addressing the cited complaint's substance

AC-2: "A query with no match shows an explanatory message naming the query, not an empty panel." This is fully satisfied by a static `"No results for 'xyz'"` label with zero fuzzy/typo tolerance or synonym handling. That's a legitimate, checkable AC on its own terms — but the issue's own motivating citation is Logisim-Evolution's "oldest findability complaint" and "no component search," which in practice includes users who don't know the exact internal name (e.g. searching "flip-flop" and expecting to find `Register`, or "mux" vs "multiplexer"). Nothing in the ACs requires the search to handle any of that; AC-1 explicitly requires only "element names and at least one alias form," which per Finding 3 currently resolves to nothing beyond exact-tag matching. A minimal literal-substring-over-tags implementation formally passes every AC while leaving most of the real-world findability gap open — this is worth calling out explicitly rather than leaving to interpretation, since the issue advertises closing a UX complaint the AC set doesn't actually require closing.

**Recommendation:** If exact-substring search is the intentionally minimal V1 scope, say so in Acceptance Criteria rather than leaving the gap implicit; otherwise add a synonym/fuzzy-match criterion.

### 6. Solid, no rework needed

- The core motivation (component search, citing Logisim-Evolution #1234) is real, specific, and traceable to #592/#521's evidence base — not invented.
- "The search index is a model-side collaborator, not a field on `SimpleEditor`" is architecturally the right instinct given #316's stated direction, even though (per Finding 1) the supporting infrastructure isn't there yet.
- "A test that fails at the pre-change commit" (AC-5) is a reasonable, standard verification discipline consistent with how the rest of this issue family is written.
- band_mw: 1 is plausible for the narrow "build an index and wire it to a text field" task *if* Findings 1-3 are resolved first; it does not itself look inflated.

## Verdict

**needs-rework.** The idea is sound and appropriately scoped as a task, but three of five acceptance criteria (AC-1, AC-3, AC-5) rest on data or documents that don't exist in the repository yet (populated element aliases, a published/scored #592 catalog), a fourth (AC-4) cites an undefined ratchet, and the issue doesn't acknowledge that its own capstone (#521) and parent feature (#594) declare a hard wait-gate on #316/#441 that is demonstrably still open. Fix the alias-source ambiguity, name a real acceptance vehicle for the K9/startup claim, and either add #316 to `blocked_by` or explicitly record the wait before this is picked up.
