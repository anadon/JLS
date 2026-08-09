# Issue #420: TASK-0053: the shipped HDL port scanners gain a consumer — a drawable black-box element whose body runs in an external simulator under a written, forward-only contract
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This is an unusually well-evidenced issue — most of its file/line citations about `src/jls/hdl/scan/` (2,566 lines split 1489/891/63/52/47/24), the zero-consumer grep, the `HdlModel.Direction{INPUT,OUTPUT}` vs `ScannedPort.Direction{IN,OUT,INOUT}` mismatch, `ElementRegistry.all()` having exactly 35 entries, and the absence of any rollback path in `src/jls/sim/` — check out exactly against the checked-out tree. That rigor makes the parts that *don't* check out, and the metadata that has visibly rotted in the five days since filing, more concerning rather than less: they are load-bearing for the Definition of Done and are wrong right now.

## Findings, most severe first

### 1. A cited acceptance-criterion test does not exist on the branch this work will actually land on (CONFIRMED, severe)

O5 and the DoD both depend on `test/jls/hdl/HdlPolicyTest.java` L394-409's `exportPolicyIsTotalOverTheElementRegistry()` and `src/jls/hdl/HdlExporter.java` L460-478's `REJECTED` bucket ("four entries"). I checked the actual repository:

```
$ grep -n "REJECTED\|classifiedElementClasses" src/jls/hdl/HdlExporter.java
(no output)
$ grep -n "exportPolicyIsTotalOverTheElementRegistry" test/jls/hdl/HdlPolicyTest.java
(no output)
$ grep -n "enum.*Bucket\|Set<Class" src/jls/hdl/HdlExporter.java
422:	private static final Set<Class<?>> EXPORTED = Set.of(
431:	private static final Set<Class<?>> SKIPPED = Set.of(
436:	private static final Set<Class<?>> TOPOLOGY = Set.of(
```

`HdlExporter.java` on this tree has three buckets (`EXPORTED`/`SKIPPED`/`TOPOLOGY`), not the four-bucket `REJECTED` totality system the issue's O5 and Completion Criteria assume. The `evidence_commit` (`2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`) doesn't even resolve in this repo (`fatal: Not a valid commit name`) — confirming the issue's own comment thread (posted the same day, referencing #493): *"This issue declares `evidence_commit` … which exists only on a branch that will not be merged and will be deleted … `HdlExporter.java:460-478` — 19 cited lines absent from `master` … `HdlPolicyTest.java:394-409` — 16 cited lines absent from `master` … Branch-only symbols … `HdlExporter.REJECTED`, `classifiedElementClasses`, `exportPolicyIsTotalOverTheElementRegistry`."*

The issue body was never edited to reflect this. Its §14 DoD still reads: *"`HdlPolicyTest.exportPolicyIsTotalOverTheElementRegistry` and `PaletteContractTest.paletteIsTotalOverTheElementRegistry` both green with the new type."* `PaletteContractTest.paletteIsTotalOverTheElementRegistry` does exist (`test/jls/edit/PaletteContractTest.java:48`), so this checklist line silently mixes one real, checkable obligation with one that currently names non-existent test infrastructure. Taken literally, an implementer either (a) cannot close the DoD item as written, or (b) must first build the missing `HdlExporter` bucket/totality-test machinery that O5 assumed was already shipped — undisclosed scope growth the issue nowhere accounts for.

**Recommendation:** before funding, re-derive O5 against the actual target branch, replace the `REJECTED`-bucket claim with what really exists (`EXPORTED`/`SKIPPED`/`TOPOLOGY`), and either scope in "add export-policy totality test infrastructure" explicitly or point the new element at whichever of the three real buckets applies.

### 2. Stale parent pointer sends the closing report to a closed issue (CONFIRMED)

`part_of_feature: 360` in the machine block, and §14's *"Landing reported on #360 (FEAT-024) with a `STATUS:` comment."* I fetched #360: `"state":"closed","state_reason":"duplicate"`, closed 2026-08-04. The issue's own comment thread contains a same-day "chain-integrity correction" (2026-08-08T18:14) saying to read `part_of_feature: 63` instead — but the body itself is unedited, so any agent or contributor who reads only the machine block (which the format explicitly tells readers to trust — "read child-before-parent") will try to post a landing STATUS to a closed, duplicate-superseded issue. Compounding this: an earlier same-day comment (2026-08-08T16:15) declared #420 itself a duplicate of #63 and slated for closure, followed 24 minutes later by a "WITHDRAWN" comment reinstating it as an open, non-duplicate child of #63. The issue's own dependency status was in active flux across the day of filing review; nothing in the body reflects any of it.

**Recommendation:** edit the body's machine block directly (`part_of_feature: 63`), not just comment about it — comment-only corrections rot silently the moment they scroll out of view, which is exactly what happened here.

### 3. A document-internal task ID collides with an unrelated, closed GitHub issue number (CONFIRMED, real hazard)

The machine block's blocked_by comment reads: *"`blocked_by: []  # TASK-0049 (bidirectional ports, FEAT-021) is NOT a blocker."* I fetched GitHub issue #49 directly: title *"Interactive simulator threading: unsynchronized control flags, Swing mutation from the sim thread, step/pause handshake races, broken run-in-background"* — closed, completed, nothing to do with bidirectional ports. Fetching #339 (FEAT-021) confirms "TASK-0049" is the planning corpus's own internal task ID for bidirectional-ports work and is explicitly "not filed" as its own GitHub issue at all. So "TASK-0049" is a document ID that numerically coincides with, but is entirely unrelated to, real issue #49. Issue #63 (this task's other close relative) separately and correctly cites *"Prerequisite #49 (simulator threading) closed completed"* — the *same* number, different referent, in a sibling document. A contributor searching the tracker for "#49" to confirm bidirectional-port status will land on the wrong, closed, unrelated issue and could reasonably (and wrongly) conclude the INOUT prerequisite is satisfied.

**Recommendation:** never let a document-internal TASK-XXXX ID share digits with a real issue number without a disambiguating marker every time it's written; here it appears only once and unqualified.

### 4. The parity acceptance criterion (P11) has a built-in escape hatch that lets the feature's central correctness claim ship unverified (gameable)

§8 Method: *"Confirm #359 (FEAT-023) has the toolchains installed and pinned in the lane, or **accept that P11 is a permanent skip and say so**."* P11 is the only prediction that actually checks the co-simulation contract agrees with reality (*"Run a black-box adder against the native adder on the batch goldens; observe agreement"*). Every other prediction (P1-P10, P12) tests loading, saving, UI states, and toolchain-absence — never that the subprocess handshake produces correct values. Per Completion Criteria, a `WAIVED:` comment naming a successor issue is sufficient to close P11 unmet. So the DoD as written permits shipping "a written forward-only co-simulation contract" whose one behavioral-correctness test was never run, and closing #63 against it anyway (§14: *"#63 closed against its §7 and its addendum's P4-P6"* — P4-P6 are failure-state UX, not correctness). This is consistent with #63 itself flagging the identical risk (*"a skipped parity test proves nothing … must be reported as such rather than as green"*), but the issue doesn't make waiving P11 harder than waiving any cosmetic criterion — same one-line `WAIVED:` mechanism for both.

**Recommendation:** either make #359/toolchain availability a hard `blocked_by` for this task (it currently isn't), or require, if P11 is waived, that the task cannot be marked "landed"/closed against #63's addendum — only "landed, parity unverified" — so a waived P11 can't quietly read as done.

### 5. Estimate almost certainly understated by the project's own accounting (feasibility)

The task's own parent context (#360, before being closed as duplicate) states: *"Band 8-14 mw. Printed task sum: 2 wk — TASK-0053 alone… the band exceeds the sum by 4x at the low end and 7x at the high end… the band prices the co-simulation harness against two external simulators and their failure taxonomy, which no task id names."* But #420/TASK-0053's own §8 Method explicitly includes exactly that harness: *"Implement `src/jls/hdl/cosim/`: harness generator, process lifecycle (spawn, kill, reap, restart), bounded stderr tail, value codec"* plus a written interop contract doc, a new dialog, five lifecycle tests, a parity test suite, and a re-scan diff-report feature — all under an issue whose own H1 says *"the two weeks belong to lifecycle, deadline handling and reaping."* The 8-14 mw band and the "no task id names it" gap in #360 describe this exact scope. Either the 2-week figure is wrong, or #360's cost analysis was wrong — the issue as filed doesn't reconcile the two, and a reviewer picking this up on estimate alone will underbudget by 4-7x on the project's own numbers.

### 6. Failure-mode table documents a path the issue's own later comment says isn't built (internal contradiction)

§7.11 lists as a handled path: *"A file class the scanner cannot handle → route to the external Yosys `write_json` extraction path, which `YosysNetlist` already parses (#63 §9). That fallback is an integration, not a parser."* But the issue's own "WITHDRAWN" comment (2026-08-08T16:39), arguing against merging #420 into #63, states plainly: *"§8 Method has no step that builds a `write_json` port extractor. Naming a route to an extractor is not building one."* So the failure-mode table documents a behavior (route-to-Yosys-fallback) that the issue's own dependency-correction comment confirms is explicitly *not* in this task's Method checklist — it belongs to #63's separate T1. If the five required lifecycle/parity tests ever exercise an out-of-scanner-subset file, there is nothing in this task's own scope to route it anywhere, contrary to what §7.11 promises a user will see.

### 7. Untestable acceptance language

§13 requires `docs/hdl-cosimulation.md` to be *"precise enough for a second implementation"* — asserted as an end-state property with no test, no reviewer checklist item, and no second implementation to check it against anywhere in §5 Predictions or §10 Falsification. As written this is a subjective bar with no way to fail it during review; it should either be dropped or replaced with a concrete proxy (e.g., "an engineer unfamiliar with the code can answer N comprehension questions about the protocol from the doc alone").

### 8. P7's "the UI thread was never blocked" has no specified assertion mechanism

§7 Method requires `aHungSubprocessIsTreatedAsCrashedWithinTheDeadline()` to assert *"the UI thread was never blocked."* ARCHITECTURE.md documents JLS's UI test harness as headless-by-default with Swing-harness layers 2/3 "reserved," so this assertion will have to be indirect (e.g., timing on a separate thread, or an EDT-responsiveness probe) — the issue names the requirement but not the mechanism, leaving room for a weak test that measures wall-clock elapsed without proving the EDT itself was never touched.

## What's solid

- The core technical grounding (O1-O4, O6-O8, the scanner's exact line counts, the three-vs-two direction mismatch, the silent-attribute-drop in `Element.setValue`, the absence of rollback in `src/jls/sim/`) is verified accurate against the checked-out tree — genuinely rare rigor for an issue at this altitude.
- The forward-only/no-rollback framing (H2, criterion 3) is a real, correctly-identified structural constraint, not an invented one — confirmed by grepping `src/jls/sim/` for rollback/withdraw/uncommit (no hits).
- Refusing `INOUT` by name rather than dropping or approximating it (H3/P3) is the right call given `HdlModel.Direction` genuinely only has two members today, and the issue correctly identifies FEAT-021 (#339) as a sequencing risk rather than mislabeling it a hard blocker.
- The five-visible-states failure surface (§7.11) is concrete, testable, and traceable to #63's addendum rather than invented fresh here.

## Verdict rationale

The technical spine is sound and the issue's citation discipline is well above the norm for this tracker, but finding #1 alone (a required, named test that doesn't exist on the real target branch, with the discrepancy already flagged in-thread and never fixed in the body) and finding #2 (closing report routed to a closed issue) are exactly the kind of defects that cause a contributor to either stall or silently under-deliver against the written DoD. Combined with the estimate mismatch (#5) and the P11 escape hatch (#4), this needs rework — specifically, re-derive the evidence block against the actual target branch and fix the machine block in-body — before it should be picked up for execution.
