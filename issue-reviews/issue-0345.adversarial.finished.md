# Issue #345: FEAT-039: a balanced-ternary machine is specified, independently emulated, assembled in-jar and drawn — and the drawn one agrees with the emulator per retired instruction
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: should-not-proceed

## 1. (Critical) The issue's sole stated beneficiary is closed "not planned," and #345 has not followed its own re-planning protocol to acknowledge it

The issue's own text is unambiguous: *"The serving capstone is descoped. #295 is this feature's only required consumer. If #295 descoped, this feature has no beneficiary and the correct response is to record that — with its cost and its path intact, not a refusal — and pause rather than silently continue."* (§7, Re-planning Protocol) and, in §1: *"#295's audience — a student or instructor who wants to see that a computer does not have to be binary… #295 (CAP-03)"* is named as the primary intended audience.

I fetched #295 directly. It is `"state":"closed"`, `"state_reason":"not_planned"`, closed 2026-08-03T23:27:46Z — the day before #345's most recent comment (2026-08-04T07:28:52Z) and five days before today. The closing comment states plainly: *"Closing as not planned… Its exclusive rows fund an ISA, emulator, assembler, monitor and filesystem that become a permanent second platform to maintain at bus factor 1… Nothing downstream blocks on this issue, so closure is free on the graph."* A follow-up comment dated **today, 2026-08-08**, explicitly reaffirms the closure and draws a table showing that the replacement capstone (CAP-39, #888) covers only a "mixed-alphabet drawing" — explicitly listing "JLS-T3 ISA, emulator, assembler" and "DOS-like monitor with a filesystem" (i.e., exactly #345's scope) as **"none"** under the replacement.

So, as of the date of this review, #345 has **zero** downstream consumer, its own protocol requires a `REPLAN:`/pause comment recording that fact, and no such comment exists on #345. This is not a hypothetical risk — it is a live, currently-true process violation. Proceeding with any of TASK-0081/82/83/84 right now means building 18-30 maintainer-weeks of code toward a goal the maintainer already evaluated and rejected on cost/bus-factor grounds five days ago.

**Recommendation:** Do not start work. File the `REPLAN:` comment #345's own §7 requires, record the descope, and close or pause #345 pending a genuine re-open trigger (which #295's closure comment already defines and which #345 does not currently meet).

## 2. (High) The feature's own cost accounting admits it is unfunded by 45-70%

§ Cost: *"Sum of this feature's own task rows: 10 wk… Printing both, because they do not agree: 10 wk of named tasks against an 18-30 mw band — a gap of 8-20 mw, a factor of 1.8x at the band floor and 3x at the ceiling… No task id names that residual."* This is an issue asking to be acted on while conceding that more than half its own declared cost ceiling has no task backing it, and that "Open Question 5" (whether the residual is funded or descoped) is unresolved. Combined with finding 1, there is now no beneficiary to justify closing that gap at all.

## 3. (High) The "independence" architecture is undermined by single authorship, and no criterion tests for it

Invariant 2 (inherited from #343) and criterion 3 in §5 test only that an *accidental* one-sided deviation is caught: *"Introduce a deliberate deviation into the emulator's division rule and confirm the corpus fails there and the drawn machine still passes."* But the specification (TASK-0081), the corpus, the emulator (TASK-0082) and the drawn machine (TASK-0083) are all authored by the same maintainer under the plan as written. A systematic *misunderstanding* of balanced-ternary semantics baked into that one person's mental model (e.g., an incorrect but internally-consistent rounding convention) would propagate into the specification, the corpus derived from it, and both implementations built against it — and none of the stated criteria would catch it, because the "seeded misreading" test only proves the two implementations are mechanically decoupled, not that either is correct. The Cost section admits this itself ("Two implementations by one author remains the feature's principal risk and no amount of testing addresses it") but then treats the conformance-corpus invariant as sufficient mitigation, which it explicitly is not for this class of error.

## 4. (Medium) The comparison's own field set — the thing acceptance rests on — is not decided

Criterion 2 in §5 requires the emulator and drawn machine to *"agree per retired instruction on the declared architectural fields,"* but Open Question 3 states: *"What is the declared architectural field set for the per-instruction comparison?… Blocks criterion 2."* Whoever picks that field set later controls, after the fact, how strong "agreement" actually is — a narrow set (e.g., register file + PC only, omitting flags, illegal-lane trap state, or memory side effects beyond "words touched") would let criterion 2 pass while behavior on the very undefined-value cases TASK-0081 is supposed to nail down goes unverified. Nothing in §5 requires the field set to cover every case TASK-0081 specifies as normative.

## 5. (Medium) Two blocking decisions are unmade, but are treated as if the plan can proceed around them

- Open Question 2 (division rule: truncate / round-to-nearest / floor) is stated to "**block TASK-0081, which is to say it blocks everything**," yet the issue offers only a "recommended default," not a decision, and provides no mechanism or deadline forcing the maintainer to actually decide it.
- Open Question 1 (native radix-3 vs. binary-encoded fallback) "must be made before TASK-0083 starts, not inside it," with a recommended trigger of "decide native if the N-ary element family has landed by then" — a condition with no concrete check (what counts as "landed"?) and no owner responsible for evaluating it at the right moment. Given #361 (the N-ary element family) is itself an open, unlanded 9-13 mw feature, this timing gate is easy to blow past silently.

Filing a "feature" issue whose first blocking dependency (the division rule) has no resolution mechanism is scope theater: the roadmap looks actionable but the actual first step cannot start.

## 6. (Medium) TASK-0038 ownership is triangulated across three issues and left unresolved

Open Question 4: *"Who owns TASK-0038? It appears in this roster, in #337's and in #326's. A task is part_of at most one feature… Blocks filing the children, and must be settled before they are filed."* The one comment on #345 resolves only the *ordering* edge (`blocked_by: [337]`), and says explicitly: *"what is still open is task ownership rather than the ordering."* So the issue's own Definition of Done item ("`planned_tasks` empty… each resolved to a filed issue") cannot be satisfied for this shared task until a cross-issue bookkeeping question three separate issues depend on is settled — a structural fragility typical of decomposing one deliverable across a fleet of parallel-filed issues with no single arbiter visible in the tracker.

## 7. (Low-Medium) Criterion 1 ("a third implementation could be checked") is unfalsifiable as stated

§5.1: *"Hand the specification and the corpus to a run that has neither implementation available and confirm the corpus is executable against an arbitrary implementation."* For a single-maintainer open-source educational tool, there is no budgeted, concrete "third implementation" anywhere in this issue's task list or cost — this criterion can only be satisfied by a hypothetical, unless the maintainer personally writes a disposable third implementation solely to check the corpus, which is not costed. As written this is gameable: it can be "verified" by argument ("the corpus imports no in-tree type") rather than by an actual independent run, and the Completion Criteria checklist would accept that as done.

## 8. (Low) Evidence commit cannot be verified in this checkout

The issue pins `evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` for all of its "verified ABSENT" claims. This repository is a shallow clone (`git rev-parse --is-shallow-repository` → true, 268 commits reachable, current HEAD `5b05d67d`), so `git cat-file -t 2d0ca9d…` fails to resolve here — it cannot be independently checked from this checkout. This is likely a shallow-clone artifact rather than a fabrication (the specific claims — no `ternary` hits in `src/`, no `docs/*t3*`, no `machines/` directory, `WireNet.propagate` at exactly line 443 — all check out against current HEAD), but the issue presents the commit as pinned, checkable evidence, and it is not checkable as filed.

## What is solid

- The scope boundary in §1 (what's in vs. explicitly deferred to #343/#324/#337/#326) is unusually well drawn and cross-referenced; the boundary-dedup comment against #361/#344 is a genuinely careful non-duplication argument, not hand-waving.
- Invariant 1 ("corpus written from the specification, never from either implementation") correctly identifies and forecloses the most obvious way this kind of parity claim gets gamed (spec-as-afterthought).
- The rejected-alternatives list in §2 ("specification alongside the emulator," "one task for the whole machine," "skip the drawn machine") shows real design deliberation rather than default decomposition.
- The line-level code citations that are checkable (`WireNet.java:443`, the `BitSet`-based value domain, absence of any `ternary`/`mach`/`machines` content) are all accurate against the current tree.

## Bottom line

Independent of its internal design quality, #345 is, right now, work planned for a capstone the maintainer closed five days ago as not worth the cost — and reconfirmed today as still not worth it, with the replacement capstone explicitly excluding this issue's entire scope (ISA, emulator, assembler, monitor). The issue itself specifies the correct response to that situation and has not taken it. Everything else here (the cost gap, the single-author independence gap, the undecided blocking questions) compounds the case that this should not proceed as filed.
