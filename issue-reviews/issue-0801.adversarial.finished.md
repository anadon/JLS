# Issue #801: TASK-C587-3: the docu-tests run against the generated targets, and four planted wrong claims are recorded as a committed negative-check record
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#801 (TASK-C587-3) closes out FEAT-C35-4 (#587) by making its docu-tests dual-target-aware (AC-4 of #587) and by proving the whole docu-test set non-vacuous with a committed four-defect negative-check record (AC-5 of #587). The instinct — a ratchet is worthless until you've watched it fail on purpose — is sound, and the task is a clean, well-scoped restatement of its parent's own two remaining ACs. But its central requirement, "every docu-test asserts against both generated targets," is not achievable for at least one of the docu-tests it inherits, its cost band assumes work its predecessor tasks never budgeted for, and three of its four "planted defects" duplicate deliverables already promised, with transcripts, by sibling tasks it barely orders after.

## Findings, most severe first

### 1. [HIGH] AC-1 ("every docu-test asserts against both generated targets") is unsatisfiable for the flag docu-test, whose sources sit outside the dual-generation pipeline

FEAT-C35-1 (#584, fetched directly) scopes the dual in-jar/site pipeline strictly to help content: "Help content is authored once... one `mvn`-reachable build emits two targets from that single source: the in-jar help tree that `resources/help` ships today... and a static site directory." Confirmed on disk: `resources/help/` (Map.jhm, JLSHelpTOC.xml, editor/, elements/, …) is the migrated tree; `docs/` (batch-interface.md, README.md at repo root) is a separate, untouched tree. But TASK-C587-1 (#799, fetched directly), the sibling task that builds the flag docu-test #801 is supposed to make dual-target, states its own AC-1 as: "A doc-facing assertion extracts flags from `docs/batch-interface.md`, the README and the help content tree and resolves each against `FLAGS`." Two of those three sources (`docs/batch-interface.md`, README.md) are never migrated to a second target by #584/#793 — there is no "site" version of them to assert against. #801's AC-1 as literally written ("every docu-test... a claim present in only one target fails rather than passing by absence") cannot be met for that test without either (a) silently exempting it, undocumented, or (b) #584's scope quietly expanding to cover `docs/` and README.md, which nothing in #584, #793, or #801 says.
**Recommendation:** scope AC-1 explicitly to docu-tests whose claims are sourced from the migrated `resources/help` tree, and state plainly that the flag docu-test's non-help-tree sources (`docs/batch-interface.md`, README) are single-target by design.

### 2. [HIGH] The dual-target retrofit is the heaviest integration work in the FEAT-C35-4 sequence, and it is unbudgeted by every task it depends on

Neither #799's AC list nor #800's (TASK-C587-2, fetched directly) mentions a second target at all — both are written and presumably estimated (0.5-1 mw each) as single-target (classpath/in-jar) checks. #801 inherits at minimum HelpTopicsTest, HotkeysHelpAccuracyTest, CliFlagTableTest's doc-facing extension (#799), and the new element-content test (#800) — plausibly five test classes — and must make each resolve claims against a second, structurally different root (a generated static-site directory) rather than classpath resources. #801's own band is `0.5-1` mw, identical to the narrowest single-target predecessor tasks, for what is actually the most cross-cutting piece of the sequence.
**Recommendation:** either re-estimate #801's band accounting for retrofitting N existing test classes to a second target, or have #799/#800 build their assertions target-agnostic from the start (an explicit `ContentRoot` abstraction) so #801 only wires the second root in, rather than refactoring five classes under its own budget.

### 3. [MEDIUM] "The hosted site" does not exist at the point this task can execute, and the loose terminology invites a network-dependent CI check

#801's ordering_after is `[TASK-C587-2, TASK-C584-3]` — it does not order after any FEAT-C35-2 (#585) publishing task. At the point #801 lands, #793 (TASK-C584-3) has produced only a generated static-site *directory* on disk; nothing has published it. Yet #801's own Outcome text calls this target "the hosted site," echoing #587 AC-4's identical phrasing. ARCHITECTURE.md's own recorded decision ("Help delivery: in-jar now, hosted docs are the planned future," lines 252-267) is explicit that hosting is future work, not what #584/#793 deliver. An implementer following "hosted site" literally could reach for a live URL fetch in a docu-test, which would violate the project's headless/offline CI discipline (ARCHITECTURE.md lines 177-181: "Batch mode never leaves the main thread... CI runs it without a display"; #584 AC-3: "no generated in-jar page depends on a network fetch to render its content").
**Recommendation:** replace "hosted site" with "the generated site directory" (or equivalent) throughout AC-1, and state explicitly that both targets are asserted against as local, generated filesystem trees — no network I/O.

### 4. [MEDIUM] Three of four "planted defects" duplicate transcripts already mandated by #799 and #800, with no statement of reuse vs. re-derivation

#799 AC-3: "a planted undocumented flag and a planted documented-but-nonexistent flag each turn CI red, with transcripts recorded." #800 AC-3: "an element page with a stale port list, and a wrong hotkey outside `hotkeys.html`... each turn CI red, with transcripts recorded." #801 AC-2 asks for the same three defect classes (undocumented flag, stale port list, wrong hotkey) plus one new one (missing element page) as part of its own "committed negative-check record." Nothing in #801 says whether it is assembling the transcripts #799/#800 already produced into one record, or independently re-planting the same defects. The former risks the record silently going stale if #799/#800's transcripts are reused verbatim without re-verification against #801's own (now dual-target) test run; the latter is duplicate authoring effort inside the same feature, three tasks apart.
**Recommendation:** state explicitly that #801 consolidates and re-verifies (not re-authors from scratch) the transcripts #799 AC-3 and #800 AC-3 already require, run once more against both targets.

### 5. [MEDIUM] The non-vacuity proof is admittedly one-time; nothing re-runs it automatically after the code it protects changes

The issue's own Outcome text: "the whole set is proven non-vacuous once, in a committed negative-check record." AC-4 makes the planting procedure "regenerable... so the non-vacuity claim can be re-established after a refactor" — but re-establishing it is a manual, on-demand act, not a scheduled or CI-triggered one. A later change that loosens one of these assertions (e.g., a refactor of `HotkeysHelpAccuracyTest`'s comparison logic) can silently make a planted-defect class untrippable, and nothing in #801 or its CI surface would notice until someone happens to re-run the planting script.
**Recommendation:** add a scheduled or pre-release CI job that re-runs the planting procedure and diffs its output against the committed transcripts, turning "regenerable" into "regularly regenerated."

### 6. [LOW] "Claim present in only one target fails" does not distinguish semantic claims from legitimate structural differences between the two targets

#584 AC-4 explicitly allows the in-jar target to be constrained to a narrower "viewer-safe subset" than the site target ("written down in-tree and enforced by the build" — implying the site can carry markup/structure the JavaHelp-derived viewer cannot). If #801's dual-target assertion is implemented as page-level diffing rather than targeted extraction of specific claims (hotkey text, flag names, port lists — which is how #799/#800 are scoped), legitimate rendering differences between the two targets could trip false failures.
**Recommendation:** state explicitly that dual-target assertions compare extracted claims (the same values #799/#800 already extract), never raw page content.

### 7. [LOW] ordering_after lists only the immediate predecessor in the C587 chain, not TASK-C587-1

`ordering_after: [TASK-C587-2, TASK-C584-3]` omits TASK-C587-1 (#799), even though AC-2's "undocumented flag" defect needs #799's flag-assertion machinery to exist to fail against. This resolves transitively (#800/TASK-C587-2 itself orders after #799/TASK-C587-1), so it is not a real scheduling gap if the chain is executed in order — but the issue is not self-auditable on this point without pulling #800's body too.
**Recommendation:** list the full predecessor set explicitly, or note that ordering_after is immediate-only by convention.

## What's solid

- AC-3 ("each failure message names the file and the contradicted source of truth") is concrete and mechanically checkable per defect.
- The instinct behind AC-4/AC-5 — commit the planting procedure, don't leave a one-shot transcript — is the right shape for keeping a ratchet honest after refactors, even though its enforcement is manual today (Finding 5).
- The task builds on real, already-shipped prior art (`HelpTopicsTest`, `HotkeysHelpAccuracyTest`, `CliFlagTableTest`) rather than inventing parallel infrastructure, and correctly names the exact extension points in each.

## Bottom line

The task is a faithful, well-cited restatement of #587's two remaining acceptance criteria, but as written it asks for something one of its own inherited docu-tests structurally cannot deliver (Finding 1), prices the actual cross-cutting integration work at the same band as its narrowest single-target predecessors (Finding 2), and leaves three of its four "planted defects" ambiguously duplicated against sibling tasks' own transcript requirements (Finding 4). None of this is disqualifying, but AC-1 needs a scope correction before this is buildable as literally stated.
