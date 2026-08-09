# Issue #635: TASK-C562-1: a .dig file's embedded test cases become -t vector files whose bytes are the same every time they are regenerated
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This task is the middle link of feature #562's chain (#635 translate → #637 report losses → #639 assert verdict parity). Its predecessor dependency is, unusually for this corpus, real and correctly cross-referenced — but `ordering_after` still names only half of what AC-3 actually needs, the invocation surface for the translation is never specified, and the Outcome text's framing ("the grading suite... become runnable `-t` vector files") oversells what a stimulus-only format can carry, the same overclaim the fleet already flagged one level up on #562 and #639.

## Findings, most severe first

### 1. AC-3 needs the Digital→JLS name mapping, which is TASK-C558-2's (#614) deliverable, not TASK-C558-1's (#612) — and #614 is absent from `ordering_after`

AC-3: *"Signal names in the emitted vectors resolve against the imported circuit's ports; an unresolvable name fails the translation loudly."* The Outcome text confirms translation writes vectors "against the imported circuit's signal names." But `docs/batch-interface.md` §2.2 requires the `-t` file's `name` token to "exactly match the name of an `InputPin` in the *top-level* circuit" — the **JLS** name, not the Digital source name. #614 (TASK-C558-2, fetched directly) is explicit that these can differ: *"Mapping is decided by semantics, with the source name treated as a hint only, so Digital's and JLS's colliding names... cannot produce a silently wrong circuit"* (AC-2: *"No mapping is decided by name alone."*). So resolving a Digital test-section signal name to the correct JLS `InputPin` name requires #614's mapping table — but `ordering_after: ["TASK-C558-1 (the parse that preserves the test sections)"]` (#612) names only the XML parse, never #614. An implementer following the stated dependency graph could start this task the moment #612 lands and have no source for the name translation AC-3 demands.

**Recommendation:** add TASK-C558-2 (#614) to `ordering_after` as a hard prerequisite, and clarify whether "the imported circuit" in AC-3 additionally requires the full import to have run (#619, TASK-C558-5) rather than just the mapping table existing.

### 2. No invocation surface is specified anywhere in the issue

AC-1 requires output "the existing batch runner accepts" and the Outcome talks about an instructor "regenerat[ing] them in a repository" — both imply a real, repeatable, mechanically-testable entry point. Nothing in the Outcome, ACs, or boundary notes names one: not a new `JLSStart` flag (the "single authoritative flag list" per `ARCHITECTURE.md` and `docs/batch-interface.md` §1), not a GUI menu action, not a standalone script. Every comparable CLI-facing feature in this codebase (per `docs/batch-interface.md` §6's stability-promise discipline and the flag-table contract) treats the invocation surface as part of the spec, not an implementation detail to be improvised later.

**Recommendation:** name the invocation surface explicitly (e.g., a new `-digtest` flag with a `CliFlagTableTest` row, or an explicit statement that this is invoked only through the `.dig` import GUI action) before this is actionable.

### 3. The Outcome text oversells what a stimulus-only `-t` file can carry — the same overclaim the fleet already flagged on #562 and #639

Outcome: *"Digital's embedded test-case sections — the grading suite an instructor's whole course rests on — become runnable JLS `-t` vector files."* `docs/batch-interface.md` §2.2's grammar (`file ::= { signal }`, `signal ::= name initial { step } "end"`) has no expectation/comparison production at all — confirmed independently by #466's own investigation (*"the word 'expect' does not appear in the document at all"*). A "grading suite" is defined by its pass/fail assertions, and those are categorically dropped by this task's channel, not merely thinned — the assertion content is #637's (TASK-C562-2) loss-report problem and the pass/fail comparison is #639's (TASK-C562-3) problem, neither of which is this task's AC. A reader of #635 in isolation could reasonably believe this task alone delivers a working grading migration; it delivers only the stimulus half.

**Recommendation:** reword the Outcome to state plainly that this task emits *stimulus* only, and that grading fidelity is a property of the three-task group (#635+#637+#639), not of this task alone — mirroring the more careful framing already used in this task's own Boundary notes ("it does not extend the runner").

### 4. No cardinality contract for a source with multiple embedded test sections

AC-1 says test sections "emit `-t` vector files" (plural, unquantified) and AC-4 only covers the zero-test-sections case ("emits no vector files and says so"). Nothing states whether N distinct embedded test sections in one `.dig` file must become N output files, a single merged file, or something else — and nothing in AC-1–AC-4 would catch an implementation that silently translates only one of several test sections while still "accepting" it under AC-1 and not tripping AC-4's zero-sections branch. Digital's test tables are also structurally row/column-oriented while `-t` is per-signal-timeline-oriented (general tool knowledge, not verified against this repo, since no `.dig`/Digital reference material exists in-tree) — a transposition step this issue never names.

**Recommendation:** add an AC (or a boundary note) stating the file-per-test-section cardinality rule, and require a fixture with ≥2 embedded test sections so the golden test can catch a silently-dropped section.

### 5. The dependency link to its parent and predecessor is prose-only, not tool-visible — but the content of the edge is, unusually, correct

`mcp__github__issue_read get_parent` on #635 returns `null` and `get_sub_issues` on #562 returns `[]`: there is no GitHub-native issue-hierarchy link anywhere in this cluster, the same class of defect the fleet already flagged on sibling tasks (`issue-reviews/issue-0637.adversarial.md` Finding 2, `issue-reviews/issue-0558.adversarial.md` Finding 6). Unlike #637's `TASK-C556-1`/`TASK-C556-2` (confirmed unfiled anywhere), this task's named prerequisite is real: #612 (TASK-C558-1) exists, is open, and its own review comment explicitly lists #635 as a downstream consumer ("Plus the unique obligation to preserve Digital's embedded test sections byte-recoverably for #635/#637/#639"). So the edge's *content* is sound — only its *mechanism* (free-text `ordering_after` instead of a native sub-issue link) is weak, same as the rest of this corpus.

**Recommendation:** low priority; fold into whatever corpus-wide link-pass eventually converts `ordering_after` to native sub-issue relationships.

### 6. No kill-criterion restated, unlike its siblings

Both #637 and #639 restate a task-level *"Kill criterion KC-29-1: stop-loss at 1.5× [band]"* in their Boundary notes; #635 has no such line despite sharing the same parent feature and band-decomposition pattern (`band_mw: "1"`, summing with #637/#639's "1"s to match #562's "2-3" band). Minor, but an inconsistency worth normalizing across the three sibling tasks.

**Recommendation:** add the KC-29-1 restatement for consistency, or explicitly note it's inherited and not repeated by convention.

### 7. AC-2's byte-determinism criterion is solid — no issue

*"No timestamps, no map iteration order, no absolute paths (FEAT-C29-5 AC-3)"* is concrete, mechanically testable, and the cited AC-3 text in #562 (fetched directly: *"Translated vector files are byte-deterministic for a given source, so regenerating them never churns an instructor's repository"*) matches verbatim. The three named nondeterminism sources show real awareness of the actual failure modes for this kind of tool. No notes.

### 8. The predecessor contract (#612/TASK-C558-1) is not vaporware — unlike some sibling dependencies in this same corpus

#612's own AC-4 already promises *"Embedded test sections survive into the parsed model unaltered, and a test asserts their content is byte-recoverable for #562's consumer"* — i.e., #635's actual input contract is already specified and owned by a real, filed, open issue, in contrast to #637's reliance on two never-filed `TASK-C556-*` prerequisites. Credit where due.

## Verdict rationale

The task's predecessor edge and byte-determinism AC are solid, and its scope carve-outs from #637/#639 are clean. But AC-3 as written cannot be correctly implemented against the stated `ordering_after` alone — it needs #614's name-mapping table, which is nowhere in the dependency list (Finding 1) — and the issue never specifies where or how this translation is actually invoked (Finding 2), while the Outcome text implies a grading-suite-parity claim that this task alone cannot deliver (Finding 3). Combined with an unaddressed multi-test-section cardinality gap (Finding 4), this needs a scoping pass before an implementer could execute it as a single coherent unit. **needs-rework.**
