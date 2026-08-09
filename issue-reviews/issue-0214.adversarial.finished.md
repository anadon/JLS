# Issue #214: In-editor test panel: a GUI front-end over the batch -t test-vector engine (Digital-parity, HDL-independent)
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is well-templated and mostly internally consistent as *originally* written, but it has been overtaken by its own comment thread in a way that makes the body actively misleading if read (as instructed) as the source of truth. Two of the six comments, both dated 2026-08-08 (the same day as "today"), unilaterally impose a hard `blocked_by: [466]` and a new `part_of_feature: 369` and strike part of the body's own §7.4/§8 — but the body's YAML machine block and "Status: Ready" line were never edited to match. On top of that, the issue's founding premise — that the batch `-t` engine already produces "pass/fail" — does not hold up against the actual code.

## Findings (most severe first)

### 1. [Critical] The body's machine block contradicts the two newest comments, and the contradiction is unresolved

Body: `blocked_by: []` … "the original 'Blocked by #91' is stale" … `Status: **Ready**`.

Comment `#5227292624` (2026-08-08 17:32): *"Add `blocked_by: [466]`. This is the one hard ordering edge this issue has."* and *"§8 step 1 … is struck — it is discharged upstream. This issue consumes the runner and must not compute a verdict anywhere in `jls.edit`."*

Comment `#5227474994` (2026-08-08 18:20, the most recent comment): restates the corrected YAML block as
```yaml
part_of_feature: 369
blocked_by: [466]
```
Neither correction is reflected in the issue body itself. A reader who follows the task instruction to treat the body as authoritative (which is exactly what this reviewer was told to do, and what §7/§14's "post-change code re-checked against §7" DoD item implicitly assumes) will build against `blocked_by: []` and a self-designed shared runner (§7.4/§8 step 1 as literally written), duplicating or conflicting with `jls.sim.TestVectorRunner`, which comment `#5227292624` says #466 owns. That is precisely the H2 "two comparison paths" drift the issue itself exists to prevent — created by the issue's own bookkeeping, not by an implementer's mistake.

**Recommendation:** Before anyone picks this up, the body's YAML block, Status line, §7.4, and §8 step 1 must be edited in place to match the two 2026-08-08 comments (or the comments explicitly retracted). Comment-only amendments to a machine-parsed dependency field are not safe.

### 2. [Critical] The founding premise — "the batch engine already shows pass/fail" — is false, verified against source

Abstract: *"JLS already runs test vectors against a circuit headlessly through the batch `-t` engine … Digital's standout UX is its built-in test-case component; matching its core (**run vectors, see pass/fail**) is a small … catch-up item because the simulation machinery already exists."*

Checked `src/jls/elem/TestGen.java` and `src/jls/elem/SigSim.java`: `TestGen extends SigSim`; `initSim` opens the file and calls `super.initSim(sim, input)`, which parses and **posts stimulus events** — there is no comparison, no expected value, no verdict anywhere in either class. `grep -c "expect" docs/batch-interface.md` → `0`. The exit-code table in `docs/batch-interface.md` §1 has exactly three statuses (0 run completed, 1 runtime failure, 2 usage error) — none means "the answer was wrong." The issue's own cited source, `docs/hdl-support-research.md:303-305`, only claims batch `-t` is "**functionally similar**" to Digital's test-case component for "Test-vector UX" — it does not claim pass/fail exists.

So H1 ("driven from an editor panel … yielding identical pass/fail results to `jls -b -t`"), P2 ("the panel's verdicts equal `jls -b -t vectors circuit.jls`'s verdicts, vector for vector"), and DoD's "CLI/GUI parity test present and green (P2)" are all built on a term — "verdict" — that has no referent in the code the issue describes as already sufficient. This is not a nitpick: it is the same defect independently identified and only partially patched by comment `#5227292624` (which redirects the panel to consume a not-yet-built `jls.sim.TestVectorRunner` from #466), but the patch never touches the body's §5/§10/§14, which still read as if a batch verdict exists today.

### 3. [High] "Status: Ready" is stale; the issue is transitively blocked on a large, unstarted task

Per finding 1, comment `#5227474994` imposes `blocked_by: [466]`. #466 ("TASK-0111") is open, unstarted, and itself large: a new `Expectations` file format, `jls.sim.TestVectorRunner`, `jls.sim.GradeReport` (byte-deterministic xUnit), two new CLI flags, a new exit status, and a full worked lab with rubric/README/three seeded submissions (confirmed via `mcp__github__issue_read` on #466 — none of `TestVectorRunner`/`Expectations`/`GradeReport` exist in the tree: `find … -iname '*TestVectorRunner*' …` returned nothing). Yet #214's body still says `Status: **Ready**` and the label set (`enhancement`, `phase:M3`, `area:batch`, `tier:task`) carries no "blocked" marker. A contributor triaging by label/Status alone will pick up work that cannot be completed or parity-tested until #466 lands.

**Recommendation:** Update the Status line and, if the repo uses a "blocked" label, apply it, so state on GitHub matches the dependency graph the comments assert.

### 4. [Medium] The issue's own cited precedent already diverges from the parity guarantee it promises

§1 cites `InteractiveSimulator.java#L593` (`gen = new TestGen(circ);`) as evidence "a GUI test driver is not new ground." Verified: `src/jls/edit/InteractiveSimulator.java` (~line 589-595) builds a `TestGen` and calls `gen.setFile(testFile)` but does **not** remove pre-existing `SigGen` elements from the circuit. `src/jls/sim/BatchSimulator.addTestGen` (~line 190-208, also cited by the issue), by contrast, explicitly iterates and removes every top-level `SigGen` before running. So the exact GUI code path the issue points to as reassurance already behaves differently from the batch path on a load-bearing point — whether the circuit being simulated is the circuit the user is looking at. The issue's own §1/§2 reading of the code it cites missed this; it only surfaced later, via #466's O5 and the "gained scope" comment's P5. A reviewer should not have needed a sibling issue to catch a divergence in code the issue itself quotes.

### 5. [Medium] Governance smell: dependency-graph and scope edits arrive only as comments, never as body edits

Three separate comments (`#5153027415`, `#5181428357`, `#5227292624`/`#5227474994`) each restructure this issue's relationship to #91, #466, and #369, including redefining what "closes" this issue means and which predictions apply. None edit the body. This is a repo-wide pattern (visible on #466 and presumably others) where an LLM-driven review process treats comments as load-bearing amendments to a YAML block that downstream tooling (and this reviewer's own instructions) treat as canonical. The risk is exactly what happened here: a fully-specified-looking body that is quietly two revisions behind its own comment thread.

### 6. [Low-Medium] P2 as literally worded in the body is gameable

Independent of finding 2's redirection to `TestVectorRunner`, the body's own wording — "the panel's verdicts equal `jls -b -t vectors circuit.jls`'s verdicts, vector for vector" — has no independent ground truth to fail against (there is no "expected value" concept in `-t`). An implementer following the body alone could satisfy P2 by having the panel echo back the same stimulus values `TestGen` posted (which will trivially agree with themselves), passing a "parity" test while delivering none of the "see pass/fail" value promised in Impact. Comment `#5227292624`'s P4/P5 additions address this by piping everything through the future `TestVectorRunner`, but the body's own §5/§14 checklist was never updated to retire the old P2 wording.

## What's solid (brief)

- Keeping the `-t` grammar frozen and treating the panel as a pure driver of existing semantics (§11, §13) is an appropriately conservative scope boundary, and is consistent with `docs/batch-interface.md`'s explicit stability-contract framing.
- The threading model in §7.9 (off-EDT run, results posted via the EDT) correctly follows ARCHITECTURE.md's documented threading discipline and cites the right precedent (`InteractiveSimulator`'s existing `TestGen` use).
- P3 (malformed vector must never `System.exit` in the GUI) is well-grounded: `TestGen.initSim`/`specError` already route through `TellUser.error` rather than `System.exit` outside batch mode, so the requirement is largely already satisfied by existing code, not a new invention — low implementation risk on that specific point.
- Retiring the stale hard block on #91 (owner adjudication, 2026-07-27/28 comments, re-confirmed against `test/jls/ui/`'s actual file list) is well-evidenced and was correctly reasoned before the #466 entanglement arrived.

## Bottom line

Do not start implementation from the body as currently written. The dependency graph (`blocked_by: [466]`) and the runner-ownership change (`jls.sim.TestVectorRunner` belongs to #466, not this issue) must be merged into the body first; separately, the body's premise that batch `-t` already has a "pass/fail" concept is false and needs correcting throughout §4/§5/§10/§14, not just patched around by a later comment.
