# Issue #872: TASK-C563-0: the combinational-cone extractor exists as one callable pass — frontier identification and a named refusal on sequential or feedback content
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the issue's premise

#872 observes (correctly, re-verified against current HEAD) that #563 (FEAT-C31-1), #565 (FEAT-C31-3) and #641 (TASK-C563-1) all say "CAP-09 (#306) owns the combinational-subgraph extractor, do not build it twice," but no filed issue actually delivers that component — #306 is a capstone with no `tier:feature`/`tier:task` child that owns cone extraction. #872 self-files the missing component under #563. The diagnosis is accurate; re-running the issue's own evidence greps against current HEAD reproduces the same "genuinely absent" result (`grep -rn "Analyze" src/ --include=*.java` matches only the VHDL-analyzer comment in `VhdlEmitter.java:15`; no `combinational cone`/`extractor` type exists in `src/`). That part is sound. The acceptance criteria built on top of it are not.

## Findings, most severe first

### 1. [Critical] AC-2's hard-coded "state-holding" element list contradicts the project's own normative classification for at least two, arguably three, of its nine named elements

AC-2 says: *"A selection containing any element that holds state — at minimum `Register`, `RegisterFile`, `Memory`, `ShiftRegister`, `StateMachine`, `Clock`, `DelayGate`, `SigGen`, `TriState` — is refused..."*

This is wrong for `ShiftRegister` and `DelayGate` by the project's own normative spec, not by my inference:

- `docs/simulation-semantics.md` §6.3 (line 243-247): *"`ShiftRegister.react` (issue #122) is a **Mux-style combinational element, not a clocked register** — the name and semantics are the bsiever fork's..."*
- `src/jls/elem/ShiftRegister.java` L21-25: *"Combinational barrel shifter (issue #122)... note that despite the name **it holds no state** — it is a Mux-like combinational element..."*
- `src/jls/elem/DelayGate.java` L10: *"Logically neutral, simply delays a signal change by a given amount."* — no doc comment anywhere calls it stateful, and `docs/simulation-semantics.md` never lists it under §8 ("Sequential semantics: edge triggering," which enumerates exactly Register §8.1, StateMachine §8.2, Clock §8.3, Memory §8.4).
- `src/jls/elem/TriState.java` is likewise absent from §8; the sim-semantics doc treats it only under HiZ/value-domain discussion (§2, §9), not sequential semantics. Its exclusion reason, if any, is that HiZ isn't representable in a two-state frontier value — which is exactly what #872's own "Value domain" boundary note describes as a *future* width/domain extension (#322), not a *current* state-holding property. Filing it under "holds state" gives the totality table the wrong justification for the right exclusion.

If AC-2 is implemented as literally written, the extractor refuses valid combinational selections containing a barrel shifter or a delay gate — both of which the project's own `HdlExporter.EXPORTED` set and normative doc treat as ordinary combinational elements alongside AND/OR/NOT gates. That directly undermines #563/#641's AC-1 ("golden-tested on at least one multi-output cone") for any circuit that happens to use these common elements, and shrinks CAP-31's demo slice for no documented reason.

**Recommendation:** strike `ShiftRegister` and `DelayGate` from the "at minimum" list, or justify the deviation from `docs/simulation-semantics.md` explicitly (a normative doc disagreement should never be silent). Reclassify `TriState`'s exclusion as a value-domain gap, not a state-holding one, so the eventual registry table's category matches its real justification.

### 2. [High] SubCircuit and FieldExtend are unaddressed, despite being exactly the elements the issue's own cited sibling excludes

#306 (CAP-09), which #872 declares `shared_with` and positions as the second consumer, says in its own body: *"The formula extractor inherits the exporter's refusals. The refusal map at `src/jls/hdl/HdlExporter.java:460-477`... is `{Memory.class,... RegisterFile.class,... FieldExtend.class,... SubCircuit.class}`... Any extractor built over the same walk inherits exactly these four refusals. That makes `SubCircuit` a day-one exclusion..."* — confirmed current at `src/jls/hdl/HdlExporter.java:460-477`.

#872's AC-2 never mentions `SubCircuit` or `FieldExtend` at all. A selection containing a `SubCircuit` (a routine structuring element in real JLS designs) is unaddressed by any of AC-1 through AC-6: does the extractor recurse into it, refuse it as opaque, or silently treat its boundary pins as ordinary frontier signals? The issue's own Outcome section promises "both consumers... cannot disagree about what 'combinational' means," but #872's refusal taxonomy (pure state-holding) and #306's refusal taxonomy (HdlExporter's four-entry structural-refusal map) do not actually overlap on `SubCircuit`/`FieldExtend`, and #872 gives no guidance for reconciling them.

**Recommendation:** add a boundary note (or AC) for hierarchy: does the cone extractor flatten through subcircuits, or refuse them, and on what basis (structural, not state-holding)? Cross-check the eventual registry table against `HdlExporter.REJECTED` as a second oracle, not just against a hand-picked "holds state" list.

### 3. [Medium] AC-2's totality-test dependency on #315 is a hidden, unordered prerequisite

AC-2 requires the state table to carry "a totality test per #315's discipline." #315 is itself an open, unimplemented feature: its own body states TASK-0001 (audit) and TASK-0002 (the reusable `RegistryTotalityTestBase` JUnit base and the `CONTRIBUTING.md` rule) are both "not filed." #872's `ordering_after` lists only `[468]` — #315 is never mentioned there or in `blocked_by`. An executor picking up #872 today has no shared base class to extend, per #315's own evidence (`git grep -l RegistryTotalityTestBase` returns zero files at its evidence commit). "Carries a totality test" is then satisfiable by any ad hoc JUnit test the author decides to call a totality test — there is no oracle to check it against #315's eventual contract, and the criterion is effectively gameable until #315 lands.

**Recommendation:** either add #315 (or its future TASK-0002) to `ordering_after`, or spell out an interim discipline with a `WAIVED:` escape hatch — the same pattern AC-6 already uses for #468's not-yet-landed status.

### 4. [Medium] The issue diagnoses a broken pointer chain but doesn't fully repair it

#872's entire premise is that #641/#642/#655 wrongly point at #306 for a component #306 never filed. Re-fetching #641 directly confirms its machine block still reads `ordering_after: [306]`, not `#872`. #872's Completion Criteria only requires that a *signature deviation* be "mirrored to #641" — it does not require updating #641's (or #642's, or #655's) frontmatter to point at #872 instead of #306. Since GitHub issue bodies are static text, not live references, #641 continues to tell the next reader "your prerequisite is #306," which is exactly the wrong-work failure mode #872's own "Why this is filed here" section warns about. The fix is filed; the pointers it was meant to correct are only half-updated.

**Recommendation:** add a completion-criteria line editing #641/#642/#655's `ordering_after` (or `shared_with`) fields to name #872, not just a `STATUS:` comment.

### 5. [Low/feasibility] `band_mw: "1-2"` looks optimistic given AC-6's own contingency

AC-6 requires using #468's not-yet-landed net-partition pass, with a fallback of writing "the private traversal... so #468 can absorb it — not so #468 has to delete it" if #468 hasn't landed. That's not a footnote — it's potentially a full net-walk implementation built to a foreign absorption contract, stacked on top of a registry-derived state table (dependent on #315, see finding 3), a cycle detector that must produce a diagnostic distinguishable from the sequential-refusal diagnostic (AC-3), and deterministic frontier ordering (AC-4). #468 itself, the dependency this task rides on, is a substantially-scoped task with no stated band in its own issue. 1-2 maintainer-weeks for all of the above, including the AC-6 contingency path, is a tight estimate; it will likely land via the WAIVED: escape hatch by default rather than the intended integration.

### 6. [Low] Taxonomy looseness: "state-holding" doesn't actually describe `Clock` or `SigGen` either

`Clock` free-runs off elapsed simulation time; `SigGen` replays a script keyed by simulation time. Neither holds a value that persists across `react()` calls in the sense `Register`/`Memory`/`StateMachine` do — their disqualifying property is "output is a function of time, not of the frontier's input values," a different refusal reason than genuine state. This is not fatal (they clearly must be refused one way or another), but AC-2's own text insists the registry table exists precisely so "a thirty-sixth element type must not silently default to 'combinational'" — if the categorical basis itself doesn't match the codebase for 4-5 of its 9 seed entries, the totality test will faithfully enforce a boundary that's already wrong at the start.

## What's solid (one line each)

- AC-4's deterministic stable-id frontier ordering matches the project's existing canonical-order convention (`Circuit.getElementsInStableOrder`, issues #165/#166) — well-grounded, not invented here.
- AC-5's headless-callable, no-`java.awt` constraint matches the established headless-core discipline (`HeadlessCoreRatchetTest`, issue #77).
- AC-6's `WAIVED:` escape hatch for #468 not having landed is a sound, precedented pattern already used elsewhere in this tracker rather than a blocking hard dependency.
- The "genuinely absent" evidence section is accurate and reproducible against current HEAD, and the boundary notes correctly keep enumeration, the input bound, the table view, minimization, and synthesis out of this task's scope.

## Verdict rationale

The dependency-graph diagnosis (finding a component two other issues assume exists but nothing files) is real and worth acting on. But the acceptance criteria that are supposed to pin the component's behavior get the domain facts wrong on the central design question — which elements are combinational — in ways directly contradicted by the project's own normative simulation-semantics document and by the sibling capstone (#306) this task claims to share code with. Implemented as written, AC-2 would ship a cone extractor that disagrees with the codebase it's embedded in about what "combinational" means, which is precisely the defect the issue's own Outcome section says this pass exists to prevent. **needs-rework**: fix the element classification against `docs/simulation-semantics.md` and `HdlExporter`'s existing buckets before this is picked up, and tighten the #315 dependency and the downstream-metadata repair.
