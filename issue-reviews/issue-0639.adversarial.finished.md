# Issue #639: TASK-C562-3: the translated vectors return the same verdicts Digital reports, asserted mechanically over a real published circuit
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This task closes out feature #562's "grading suite survives migration" claim with a Digital-vs-`-t` parity harness. The sequencing against its siblings (TASK-C562-1 #635, TASK-C562-2 #637, TASK-C558-5 #619) is coherent, and AC-2's anti-vacuity clause is a genuine improvement over the parent feature's own gameable AC-4 (see `issue-reviews/issue-0562.adversarial.md` Finding 4). But the core deliverable — "verdict parity... pass for pass and fail for fail" — is asked of a channel (`-t`) that the project's own in-flight infrastructure work documents as having no verdict concept at all, and the issue's Boundary note both disclaims building that concept and fails to depend on the one open issue that would build it. As written, an implementer has two structurally different ways to satisfy AC-1/AC-2, with very different costs, and the issue does not say which is intended.

## Findings, most severe first

### 1. AC-2 demands "verdict parity" from a channel with no verdict concept, and the Boundary note both disclaims and depends on the fix

AC-2: *"a test Digital fails must fail under `-t`, and a test Digital passes must pass."* The Boundary notes: *"This asserts over the existing `-t` runner; it does not extend it (#466, #214 own the runner and the panel)."*

`docs/batch-interface.md` §2.2's grammar (`file ::= { signal }`, `signal ::= name initial { step } "end"`) has no expectation production, and §1's exit-status table has exactly three rows (0 run completed, 1 runtime failure, 2 usage error) — none meaning "ran and disagreed." Issue #466 (TASK-0111, open, cited in this task's own boundary note) states this as its entire premise: *"the batch engine has no verdict at all... per-vector pass/fail is not a UI task over an existing verdict — the verdict does not exist yet"* (O1–O3 in #466, reproduced against the live tree at a pinned commit). So "the existing `-t` runner" that this task asserts over cannot itself report pass/fail — it can only replay stimulus and print watched-element values (`Register`/`Memory`/`OutputPin`, §3.2). The boundary note names #466 only to say this task will *not* build what #466 would build, yet #466 does not appear in `ordering_after: ["TASK-C562-1", "TASK-C562-2", "TASK-C558-5..."]`. This is the identical structural defect the fleet's own review already found one level up, on the parent feature (`issue-reviews/issue-0562.adversarial.md` Finding 1: "#466... appears nowhere in the machine block, only in a boundary-note aside"). It has propagated unfixed into the task that is supposed to actually execute the parity check.

**Recommendation:** either add #466 to `ordering_after` as a hard prerequisite (if the intent is to compare against a real batch-mode verdict), or — see Finding 2 — explicitly specify that the comparison logic is a test-only harness built outside the product's CLI/contract surface, and say so in the Outcome text rather than leaving it implicit.

### 2. Two structurally different implementations both satisfy the letter of AC-1/AC-2, at very different costs, and the issue doesn't choose between them

Given Finding 1, there are two readings:

- **(a) Wait for #466's `-check`/`TestVectorRunner`/exit-3 verdict**, and drive that through the CLI. Real cost, currently blocked on an entire unstarted task (§7 of #466 lists five new classes and a worked lab, sized "1 maintainer-week" on top of everything else in that sweep).
- **(b) Build a bespoke, test-only comparison** — a JUnit test that runs `-t` via `BatchSimulator`, captures watched-element stdout, and diffs it against hand-recorded expected values per vector — exactly the pattern `examples/autograde/autograde.py`'s `EXPECTED_STDOUT_LINES` and `test/jls/AutogradeBridgeExampleTest.java` already use today (confirmed present in the repo). This route needs no product change and technically satisfies "asserts over the existing `-t` runner; it does not extend it."

Route (b) is almost certainly the path of least resistance for an implementer under schedule pressure, and it is indistinguishable from AC-1/AC-2's letter — a harness that hardcodes "expected line N is `Output Pin q: 5`" per Digital test case *is* a mechanical pass/fail assertion "over the existing `-t` runner." But it is also exactly the string-diff pattern `docs/capability-roadmap/sweep-04-verification.md` (read in this pass) singles out as the thing #466 exists to replace ("today grading means diffing stdout strings... that is the current state of the art"). The Outcome text's ambition — "the claim that a course's grading suite survived migration stops being a hope" — is a much stronger claim than "we wrote one more `autograde.py`-style fixture-diff test," and the issue gives no criterion to tell a reviewer which one was actually delivered.

**Recommendation:** state explicitly in the Outcome or an AC which implementation shape is intended (own-harness diff vs. `-check` verdict via #466), and if it's the former, say so plainly rather than letting "verdict parity" imply more rigor than a string diff provides.

### 3. AC-4's forensic detail ("which test, which vector line, and which signal differed") presumes structure the disclaimed-extension boundary doesn't guarantee exists

AC-4 requires a disagreement report naming the test, the vector line, and the signal. Under reading (b) above (Finding 2), this requires the harness itself to correlate `-t` file line numbers with simulated-time watched-element output — nontrivial bookkeeping that belongs to no upstream issue and isn't budgeted here (`band_mw: "1"` for the whole task, the smallest unit size in this family, per the C562 series' own convention of `band_mw: "1"` per task). Under reading (a) (via #466), this granularity is closer to free — #466's own `GradeReport` design already carries `(expectation, observed, passed)` records with location. The two readings again diverge sharply in cost, reinforcing Finding 2: the issue can't be costed at "1" without knowing which one is meant.

**Recommendation:** fold into the Finding 2 resolution — once the implementation shape is fixed, re-derive AC-4's cost against it.

### 4. No requirement that the fixture reused here is the same circuit whose import was already validated by TASK-C558-5

`ordering_after` includes `"TASK-C558-5 (the circuit under test must import)"` (#619), whose own AC-1 requires "one real published `.dig` circuit imports with zero unexplained losses." #639's AC-1 separately requires "at least one real published `.dig` circuit with tests." Nothing in #639 states these must be the *same* circuit. If an implementer picks a different `.dig` fixture for the parity check than the one #619 already proved imports cleanly, #639's harness inherits an unvalidated import path — a circuit that partially fails to import (silent-loss risk that #619 exists to rule out) could still produce a "passing" verdict-parity result on whatever subset of signals happened to translate, defeating the Outcome's stated purpose ("migrating circuits without their tests migrates half a course... migrating tests without checking their verdicts migrates a suite nobody can trust" — this task can pass while doing exactly that, silently, on the *circuit* side).

**Recommendation:** add an AC or boundary note requiring the same fixture circuit used for #619 (TASK-C558-5)'s import proof, or explicitly justify why a different circuit is acceptable.

### 5. Kill-criterion stop-loss is ambiguous about which band edge it multiplies

Boundary notes: *"Kill criterion KC-29-1: stop-loss at 1.5× #562's 2-3 mw band."* 1.5× of 2 mw (3 mw) or 1.5× of 3 mw (4.5 mw)? This is the same ambiguity already flagged against a sibling task (`issue-reviews/issue-0558.adversarial.md` Finding 7, re KC-29-1 there): *"an implementer could burn anywhere from 6 to 9 mw before anyone can point to the criterion and say 'kill it now'."* Here the spread (3 vs 4.5 mw) is proportionally just as loose relative to this task's own `band_mw: "1"`.

**Recommendation:** state explicitly which edge the multiplier applies to, or replace with a measured trigger.

### 6. AC-3's "committed fixture with provenance" doesn't specify how Digital-side verdicts were produced or how reproducibility is checked

AC-3 requires the Digital-side verdicts be "a committed fixture with their provenance (source circuit, Digital version), so the comparison is reproducible without Digital installed in CI." This only guarantees the *comparison* step is reproducible (diffing two files); it says nothing about whether the *recording* step is trustworthy or repeatable — e.g. whether the "Digital version" note is a free-text comment or an enforced/pinned value, and whether there's any process for re-generating the fixture if the source `.dig` circuit or Digital's own test semantics change. A hand-run, hand-transcribed verdict list committed once, with no regeneration script, is common practice but is also exactly the kind of unreproducible provenance the project's stated reproducibility ethos (README's bit-for-bit jar/BOM discussion) would flag elsewhere. This compounds the licensing/attribution gap already raised against the parent feature for the same class of "committed real published circuit" fixture (`issue-reviews/issue-0562.adversarial.md` Finding 5), which #639 inherits without addressing.

**Recommendation:** require either a script (even a documented manual procedure) for regenerating the Digital-side fixture, or explicitly accept it as a one-time, human-verified artifact and say so.

### 7. Sequencing against siblings is sound — no issue

`ordering_after: [TASK-C562-1, TASK-C562-2, TASK-C558-5]` correctly requires translated vectors to exist (#635), untranslatable-construct losses to be reported rather than silently dropped (#637), and the source circuit to already import cleanly (#619) before parity is checked. That's the right order and each edge is a real dependency, unlike some sibling issues in this corpus that use free-text `ordering_after` without any issue-number cross-reference at all (contrast `issue-reviews/issue-0562.adversarial.md` Finding 3) — this issue's own `ordering_after` values are, notably, task-id strings rather than bare numbers either, so it shares that minor convention gap, but the *content* of the edges is correct.

### 8. AC-2's anti-vacuity clause is a genuine, well-targeted fix relative to the parent feature — no issue

*"A suite that passes everything is a failure of this task, not a success"* directly closes the gameability hole the fleet flagged against #562 itself (`issue-reviews/issue-0562.adversarial.md` Finding 4: "an implementer can satisfy AC-2 and AC-4 simultaneously and vacuously by choosing a single small fixture... no evidence the translator handles anything harder"). Requiring bidirectional agreement (Digital-fail → `-t`-fail, Digital-pass → `-t`-pass) over a real circuit with real failing cases is a meaningfully harder, non-gameable bar. Credit where due.

## Verdict rationale

The task inherits, unresolved, the same load-bearing contradiction the fleet already identified one level up in #562: "verdict parity" is asked of a batch interface that has no verdict concept, the one issue that would supply it (#466) is disclaimed rather than depended on, and the issue is silent on which of two very differently-costed implementation strategies is intended — a genuine ambiguity, not a nitpick, because it changes both the true dependency graph and whether AC-4's forensic detail is achievable at the stated `band_mw: "1"`. Combined with the unstated same-fixture requirement against TASK-C558-5 (Finding 4) and the unresolved provenance/regeneration question for the Digital-side fixture (Finding 6), this needs a scoping pass before an implementer can execute it as a single coherent unit. **needs-rework.**
