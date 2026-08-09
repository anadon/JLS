# Issue #466: TASK-0111: batch mode gains a verdict — a separate expectations file, one shared runner behind the CLI and a GUI panel, a byte-deterministic xUnit report, exit status 3, and one complete worked lab
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The technical grounding is genuinely solid — every file:line citation I re-checked against the working tree (exit-status table, `-t` grammar productions, `grep -c "expect"` = 0, 14-entry `FLAGS` table, absence of `-check`/`-report`, `src/jls/sim/` contents, `TestGen extends SigSim`, `test/jls/ui/` file count = 34, `LogicElement`'s sealed permits) matches exactly. The problem is not the engineering content, it is that the issue has been rewritten out from under itself twice by same-day, same-author comments, and the body and its own third comment now disagree about what #466 is scoped to build. An implementer opening this issue today cannot tell, from the artifact alone, whether they owe a `TestPanel`, a `grade.py` directory-grading command, or a worked lab — three deliverables the body's own §6/§8/§14 mandate and the newest comment explicitly forbids.

## Findings, most severe first

### 1. Body and its own "REVISION" comment give contradictory Definitions of Done — the artifact is currently self-inconsistent

The issue body's §6 Materials, §8 Method and §14 DoD require building `src/jls/edit/TestPanel.java`, wiring it to a menu action, and `examples/autograde/lab-01/` with a full rubric, instructor README and `grade.py` that grades a directory of submissions. Comment 3 (`issuecomment-5227290058`, posted the same day) declares:

> "**No `jls.edit` code in this PR** — a `TestPanel` appearing here means the split was ignored"
> "**No submission-directory grading command in this PR** — that is #757"

GitHub does not let a comment edit the body's checkboxes; both the original §14 list and comment 3's "Revised Definition of Done" are live, uncrossed-out, and contradictory. A contributor who implements strictly against the visible body (still the top of the issue) builds a `TestPanel` and a `grade.py` cohort command that comment 3 says do not belong here. A contributor who reads only the body ignores the revision entirely, since nothing marks the body superseded except a later comment's own say-so. **Recommendation:** the maintainer must edit the issue body directly (not add a fourth comment) so the DoD a reader sees first is the one that governs, or close and refile a clean, single-source issue.

### 2. The revision comment's own "worked lab" carve-out contradicts its own DoD checklist

Comment 3, §2c, states in prose that #466 "retains one minimal worked lab — a circuit, a `-t` vector file, an expectations file, and three seeded submissions — **solely as the conformance fixture for P11** and for `AutogradeBridgeExampleTest`." But the same comment's "Predictions retained" list in §2a is `P1, P2, P6, P7, P8, P9, P10` — **P11 is absent** — and its own "Revised Definition of Done" checklist likewise never mentions P11 or a worked-lab fixture. So within one comment, the prose promises a fixture and the checklist that actually gates completion does not require it. As written, an implementer can tick every box in the Revised DoD while shipping zero worked-lab fixture, directly at odds with the "solely as the conformance fixture for P11" sentence two paragraphs above it. This is a textbook gameable-acceptance-criteria defect: the enforceable list and the stated intent diverge, and the enforceable list is what a reviewer will actually check against.

### 3. The body asserted a hard blocking dependency that its own cited issue had already retired six days before filing

The body's Status block calls TASK-0021 "a genuine hard prerequisite for the panel half" and cites #214 as the source: *"#214 names #91 as a real blocker in its own Status section."* But #214 (fetched directly) records, in its own Status block dated **2026-07-27/2026-07-28**: *"former hard block on #91 retired — the harness capabilities this task needs are on master,"* and lists `blocked_by: []`. #466 was filed **2026-08-03**, six days after #214's blocker was already retired on the record. The body's own load-bearing dependency claim was stale at the moment of filing, not merely by the time of review — a basic "read the issue you're citing" failure. Comment 3 eventually catches and corrects this (§2b), but the body itself never was, and a reader trusting only the body inherits a false blocker.

### 4. The dependency graph is unstable across #466's own comment thread within a single day

Comment 1 (`5174164484`, 2026-08-04) states: *"#524 ... orders behind this issue and #369."* Comment 3 (`5227290058`, 2026-08-08) reverses this: *"the thing those consumers actually depend on is #466 alone, and #466 depends on nothing. The `#369` half of each of those edges should be dropped."* Both statements were made by the same author on this same issue's thread, four days apart, about the same edge. If the ordering graph attached to #466 flips polarity on its own comment thread, it cannot be treated as settled input for scheduling #524/#686/#757 without independent re-verification — and comment 3's claim that "corrections posted on #524, #686 and #757" actually landed is unverified from #466 alone.

### 5. Splitting P3/P4/P5 (the CLI/panel parity contract) into #214 removes the only test that would catch a `TestVectorRunner` API that's unusable from Swing

Per comment 3, `GradingParityTest` and its predictions (P3: verdict-list equality, P4: located non-fatal panel errors, P5: substitution notice) all move to #214, along with H2. But `TestVectorRunner`'s public signature — the thing §7.9 in the body requires to be "synchronous and headless... safe to call from a CLI `main` and from a Swing worker" — is built here, in #466, with no acceptance test in #466 checking that the signature is actually panel-usable. The parity test that would have caught a CLI-convenient-but-Swing-hostile API design now lives one issue downstream, after #466 has already shipped and its public class is presumably stable. This is a real feasibility risk the split introduces: #466 can pass its own (now headless-only) test suite while quietly building an interface #214 cannot consume without a breaking change back into #466.

### 6. `evidence_commit: 2d0ca9d` does not exist in this repository at all

Verified directly: `git cat-file -t 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` returns `fatal: … could not get object info`, and the commit does not appear in `git log --all`. Every permalink in Observations O1–O9 of the body (`.../blob/2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7/...`) is a 404. This also means the issue's own §14 DoD item — *"Every cited evidence document and permalink resolves on the default branch at close — no branch-path links, no deleted docs"* — was **unsatisfiable from the moment the issue was filed**, not just later. Comment 3 catches this and re-anchors to `29afb26`, which I independently re-verified holds for O1–O9 (see Summary). Solid catch by the process, but it means the body as filed shipped a self-falsifying completion criterion.

### 7. Exit-status semantics (H4) are sound and well-guarded — no notes

The rule that expectations-file parse errors are a usage error (2), never a check failure (3), including on error paths, is unambiguous, testable, and the rationale ("a broken grader scores every submission as wrong") is correct and well-argued. This part of the design is not in dispute.

### 8. Compatibility gate (P9/P10, the `-check`-less path) is concretely testable and does not overclaim

"A run without `-check` prints the same bytes and returns 0/1/2" is checked against real, existing goldens (`BatchSimulationGoldenTest`, both `VcdExportGoldenTest` goldens) that already exist in `test/jls/` and are not being asked to move. This is a legitimate, low-risk regression gate, correctly identified as the compatibility proof rather than merely asserted.

### 9. The xUnit determinism requirement (H5/P6/P7) is reasonable but the fallback is underspecified in the retained scope

§10's falsification branch for H5 says if a consumer rejects a report lacking `timestamp`/`hostname`/`time`, the fix is "fixed, documented placeholder values… the answer must be in §2.5 rather than in the writer's code only." That's a sound principle, but §2.5 (the expectations grammar section of `docs/batch-interface.md`) is documentation for the *input* grammar, not obviously the right home for xUnit *output* placeholder values — the issue conflates "goes in the doc" with "goes in this specific subsection." Minor, but worth a reviewer flag before someone writes doc text in the wrong place.

## Verdict rationale

The underlying engineering plan (separate expectations file, one shared runner, gated `-check`/`-report` flags, byte-deterministic xUnit, exit 3 only under `-check`) is coherent and its factual claims about the codebase check out. But the issue as it currently stands is not a single executable specification: the body and its "REVISION" comment give conflicting DoDs, the revision comment contradicts itself on whether a worked-lab fixture is required, and a load-bearing dependency claim in the body was already false at filing time. None of this is fixable by the implementer choosing one reading over another — it requires the maintainer to consolidate the body itself before this is safe to pick up. **needs-rework.**
