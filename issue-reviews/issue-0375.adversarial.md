# Issue #375: TASK-0002: a thirty-sixth element type fails the build in every registry-keyed table, because one JUnit base enforces totality and CONTRIBUTING makes extending it mandatory
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The core technical narrative is real and I verified it directly against the working tree: `ElementRegistry.java` currently defines 35 element types, `BuiltinElementRenderers.java` registers only 33 of them into `ElementRenderers`, and only 31 into `ElementDialogs`; the three missing tags in both cases are exactly `FieldExtend`, `RegisterFile`, `TestGen` (renderers) and those plus `WireEnd` (dialogs), and `RegisterFile`/`FieldExtend` are confirmed live palette rows (`src/jls/edit/Palette.java:156,160`) outside the one documented exemption set (`test/jls/edit/PaletteContractTest.java:44-45`). No `RegistryTotalityTestBase` exists anywhere in `test/` or `src/`. The proposed remedy — one abstract JUnit base with `covered()`/`exempt()`/`remedy()` — is a reasonable, proportionate design. But the issue has a foundational factual defect in its own evidence, an unresolved and effectively unfileable blocking dependency, and a scope gap between what the CONTRIBUTING rule promises and what the task actually builds.

## Findings, most severe first

### 1. O7's central claim — "four existing totality checks" — is false on the actual tree, and the issue's own author has already flagged this issue by number

O7 asserts: *"the existing checks are four independent implementations"*, citing `test/jls/hdl/HdlPolicyTest.java:392` (`exportPolicyIsTotalOverTheElementRegistry`) and `test/jls/ElementRegistryTest.java:124` (`everyWritableRegisteredTagIsInTheFrozenTagTable`) alongside `PaletteContractTest.java:48`. I grepped the current checkout for both symbols and neither exists:

```
$ grep -n "everyWritableRegisteredTagIsInTheFrozenTagTable" test/jls/ElementRegistryTest.java   # no match
$ grep -n "exportPolicyIsTotalOverTheElementRegistry" test/jls/hdl/HdlPolicyTest.java            # no match
$ grep -n "paletteIsTotalOverTheElementRegistry" test/jls/edit/PaletteContractTest.java
48:	void paletteIsTotalOverTheElementRegistry() {
```
Only 1 of the 4 cited "existing" checks is real; the other cited file, `test/jls/hdl/HdlExporter.java`'s `REJECTED` bucket that the export-policy check depends on, doesn't exist on master either. This is not a stale-line-number issue — it's a whole test method and the production code it tests that never landed on `master`. The issue's own companion comment (posted by `anadon`, referencing issue #493) concedes exactly this: *"on `master` only `PaletteContractTest.paletteIsTotalOverTheElementRegistry:48` is actually present. The other two named there are branch-only."* I independently confirmed via `git merge-base --is-ancestor` that `evidence_commit` `2d0ca9d` is **not** an ancestor of `master`. Issue #493 itself lists **#375 by number** in its "Wrong about master — quotes or relies on branch-only code" bucket.

**Why this matters beyond citation hygiene:** H1 ("The four existing totality checks differ only in three values ... so all four can be rewritten as subclasses of one base with no change in what they assert") is framed as a falsifiable hypothesis about *four* things; only one of the four exists to test H1 against. The § Method checklist instructs "Rewrite `HdlPolicyTest.exportPolicyIsTotalOverTheElementRegistry`" and "Rewrite `ElementRegistryTest.everyWritableRegisteredTagIsInTheFrozenTagTable`" — both instructions target code that isn't in the tree. A contributor following the checklist literally cannot complete two of its eight rewrite steps; they'd have to first discover (as I did) that #488 and #492 are the issues that actually own landing that code, then decide unilaterally whether to pull that dependency in or silently drop two of the "four" rewrites.
**Recommendation:** Re-scope § Method to the checks that actually exist on `master` (`PaletteContractTest`, the two `ElementVocabularyTest` checks I confirmed at lines 49/121), explicitly add `blocked_by: [488, 492]` for the HDL-policy and frozen-tag-table rewrites, or drop those two bullets and retitle O7/H1 to match reality.

### 2. The single explicit blocking dependency does not exist as a filed issue, so the task is not actually executable today

The issue states: *"TASK-0001 (audit and pin every registry-keyed table) must land first... TASK-0001 is being filed concurrently with this issue, so its number does not exist yet."* I searched the tracker (`search_issues` for the TASK-0001 title text) and found zero results, and confirmed `docs/registry-keyed-tables.md` — TASK-0001's sole deliverable — does not exist anywhere in the repo. The parent feature #315 lists TASK-0001 as `"not filed"` in its own roster as of the last update (2026-08-08, same day as this issue). § Method step 2 reads *"Read TASK-0001's landed `docs/registry-keyed-tables.md` and fix the base's three abstract members against what it actually records"* — there is nothing to read. Open Questions 1 and 2 are both marked "Blocks execution" and "answerable at TASK-0001's inventory review" — a review of a document that does not exist.
**Recommendation:** Either file TASK-0001 first and let this issue sit genuinely blocked until it lands (update `blocked_by` with a real number, not a promise), or demonstrate the base class can be built and validated without the inventory (e.g., against just the two known tables) and defer the CONTRIBUTING-wide rollout to a follow-up once TASK-0001 exists. As filed, a contributor picking this up hits a hard stop at step 2 of 14.

### 3. The D6 sequencing waiver cites a document that is one of the 195 branch-only planning files and does not exist on master

The issue justifies landing ahead of "the core extraction" by citing *"maintainer decision D6... recorded in `docs/plan/evidence/BRIEF.md` §12, which landed in `3a81a4a7d6a0f108ec201e632732d308cc02b3fc`"*. I confirmed `docs/plan/evidence/BRIEF.md` does not exist anywhere under `docs/` in the working tree (`find docs -iname "*.md" | grep -i plan` returns nothing), and per #493 §2, `docs/plan/**` is exactly the 195-file bucket that is "Unrecoverable by re-reading" — never existed on `master`. The quoted operative clause ("Defect fixes are NOT gated on the core extraction") is therefore sourced from a document a reviewer on `master` cannot verify at all, only trust verbatim from the issue body.
**Recommendation:** Either attach the D6 text as an issue comment (so it survives independent of the dead branch) or get the maintainer to restate the sequencing waiver directly in this issue/its parent, rather than pointing at an unrecoverable citation.

### 4. CONTRIBUTING rule scope exceeds what this task builds or demonstrates — Orientation- and EditOp-keyed tables have no worked example

§7.7 commits the new CONTRIBUTING bullet to *"any table keyed on the element registry, an `Orientation`, an `EditOp` or a save tag"*. Every concrete deliverable in § Method — the base class shape (`Set<String> covered()`/`exempt()`), the six rewritten/added subclasses, the self-test — is scoped exclusively to element-registry tag sets. H2 interrogates only the `Class`-vs-tag question for element types; nothing in the Hypothesis, Predictions, or Method sections tests whether `Orientation` or `EditOp` key sets even fit the same `Set<String>`-based `covered()`/`exempt()` contract (an `Orientation` is an enum with a fixed, small, compile-exhaustive set — arguably a category where a runtime JUnit totality check is the wrong tool entirely, since `switch` exhaustiveness already covers it). A contributor who takes the new CONTRIBUTING sentence literally when adding an `Orientation`-keyed table has no subclass to copy and no evidence the base's shape even accommodates their key type.
**Recommendation:** Narrow the CONTRIBUTING bullet to what this task actually validates (element-registry-keyed tables and save tags), or add a worked `Orientation`/`EditOp` example before making the rule mandatory for those key types.

### 5. Acceptance path for the two newly-discovered gaps is honor-system, not verified — a real "green while the real goal fails" route

Open Question 1's recommended option is *"this task adds only the exemptions and files the genuine gaps"* for `ElementRenderers`/`ElementDialogs`, i.e., codify `RegisterFile`/`FieldExtend` as `exempt()` rather than fix the missing renderer/dialog. The Completion Criteria checkbox for this is only *"the renderer/dialog gaps found in O3/O4 are filed as new issues, not fixed here"* — a PR-description assertion, not a test. Nothing in the base class or `ArchitectureRulesTest` verifies that a follow-up issue was actually opened, or blocks the exemption from becoming permanent. This is exactly the failure mode the issue exists to prevent (a silent, uncovered gap that "the build stays green" over) reproduced one layer up: the new tests can go green by exempting the two live incidents (`970db41`/`b54e6ee`'s siblings) rather than by anyone being forced to fix them.
**Recommendation:** Require the exemption entries for `RegisterFile`/`FieldExtend` in `ElementRenderers`/`ElementDialogs` to carry a linked, filed follow-up issue number in the exemption reason string (machine-checkable format), or make `ArchitectureRulesTest` cross-reference the inventory's exemption reasons against open issues.

### 6. Solid, worth stating briefly

- O1–O6's numeric claims (35/33/31, the exact missing-tag sets, the `Class`-keyed `Map` shape at `ElementRenderers.java:24-25` and `ElementDialogs.java:25-29`) are all independently verified accurate against the current tree — this part of the evidence is trustworthy.
- The `covered() = exact-equality, not containment` design (§7.10) correctly avoids the weaker containment check that would miss stale rows; this is a genuinely sound test-design choice.
- Explicitly disclaiming closure of #78 (compile-time contract) in § Threats to Validity is honest scoping and prevents an easy overclaim.
- `final` on the base's `@Test` methods to prevent silent weakening by subclasses is a good, cheap safeguard.

## Verdict rationale

The mechanism design (abstract JUnit base, equality-based totality, self-test proving the base can fail) is sound engineering and the underlying defect is real and reproducible. But the issue is not ready to execute as filed: its own evidence bucket (O7) is partly fabricated relative to `master` and already flagged by the author's own tracking issue (#493) by number, its sole blocking dependency (TASK-0001) doesn't exist as a filed issue despite being load-bearing for the base class's shape (H2), and its sequencing waiver (D6) cites an unrecoverable document. These are fixable without redesigning the approach — hence `needs-rework` rather than `should-not-proceed`.
