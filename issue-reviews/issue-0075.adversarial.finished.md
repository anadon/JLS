# Issue #75: Keyboard operability and accessibility: accelerators, focus model, shared Actions, and keyboard-only construction landed — residual: File>Close accelerator, GUI HDL-export entry, assistive-tech pass
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

Unusually for this fleet, the issue's technical citations check out almost
perfectly against HEAD: `EditOp` has exactly 18 constants
(`src/jls/edit/EditOp.java:35-69`), `JLSStart.java` has exactly 42
`setAccelerator`/`setMnemonic` call sites, the mask+W→Watch /
plain-W→wire-start split is real (`SimpleEditor.java:1360-1370`,
`:1671`, `:1956`), `File>Close` genuinely ships with no accelerator today
and the comment explaining why is verbatim at `JLSStart.java:1548-1554`,
and `EditorKeyboardConstructionTest.keyboardBuildsTheTwoGateCircuit` is a
real test at line 134. The origin-audit permalink
(`ERGONOMICS-AUDIT-2026-07.md` at `6f5bfc5`) also resolves via `git show`
even though the file is gone from the tree. The engineering claims are not
the problem here. The problems are in what the issue's own process lets it
close *without* delivering, and in an unresolved decision that has been
sitting untouched far longer than its stated blocker justifies.

## Findings

### 1. (High) The Definition of Done lets #75 close without shipping either residual named in its own title

The title promises three residual items: "File>Close accelerator, GUI
HDL-export entry, assistive-tech pass." Only one of the three
(`#288`) is in the hard gate:

> `requires_tasks: [288]`
> `planned_tasks: [File>Close accelerator, Assistive-tech manual pass]`

And the DoD's first checkbox:

> "Every entry in `requires_tasks` closed as landed, or descoped via a
> `REPLAN:` comment... `planned_tasks` empty (each resolved to a filed
> issue or descoped)"

A `planned_task` is satisfied by *filing it elsewhere* or *descoping it in
a comment* — not by landing it. So #75 can go green and close while the
File>Close accelerator still doesn't exist and no VoiceOver/Orca pass has
ever run, as long as someone writes two comments pointing at new issue
numbers. Given this repo's own visible pattern (#288 itself is a
scope item that migrated from #60→#59-ledger→#75 without the underlying
work ever landing on any of those three), "filed elsewhere" has already
proven to be a multi-month holding pattern here, not a fast follow.
**Recommendation:** move the File>Close accelerator and the AT pass into
`requires_tasks` (file them as real child issues now, the same way #288
was) so DoD bullet 1 can't be discharged by paperwork alone, or explicitly
state in the Abstract that "closed" will not mean "keyboard/AT-accessible
end to end."

### 2. (Medium) Open Question 1 has a recommended answer that has sat un-adjudicated for a month

The mask+W/Watch decision blocking the close-accelerator slice was
identified with a recommended resolution ("(a) move Watch to another
stroke... **recommended**") as of the 2026-07-19 hold comment. The issue
has had at least four subsequent owner adjudication comments (2026-07-27,
2026-08-02, 2026-08-04) touching other topics on this same issue, but this
one-line, low-risk, already-recommended decision was never taken. As of
today (2026-08-09) `File>Close` has shipped with **no keyboard
accelerator at all** for three weeks, on the very issue whose Abstract
promises "every operation reachable... with platform-correct
accelerators." That's a live regression against the issue's own stated
capability statement, sitting behind a decision nobody is blocked from
making. **Recommendation:** adjudicate Open Question 1 now (it's already
marked "blocks nothing else") rather than leaving Close permanently
without a shortcut.

### 3. (Medium) I3's acceptance bar is unfalsifiable, and the issue tracker itself says so

> "I3 (outstanding): VoiceOver and Orca announce menus and dialogs
> **meaningfully**."

"Meaningfully" has no rubric, checklist, or pass/fail criterion attached
— contrast with I1/I2, which cite concrete tests
(`MenuAcceleratorFiringTest`, `keyboardBuildsTheTwoGateCircuit`). This
gap is not just my read: the 2026-08-04 dedup comment on this very issue,
comparing #75 to #549, states outright that #549 "carries a falsification
requirement #75 has nothing like: a seeded keyboard-unreachable dialog
must produce a recorded red run before any ratchet pass is counted." The
issue that is supposed to close out AT verification for this codebase
concedes, in its own comment thread, that its manual-pass criterion can't
fail. **Recommendation:** borrow #549's seeded-defect-must-go-red
discipline for I3 before recording it as satisfied, or explicitly narrow
I3 to a checklist of concrete announcement checks (menu label read,
mnemonic read, disabled-state read, dialog title read) instead of
"meaningfully."

### 4. (Low) Process overhead is now large relative to the remaining work

The remaining substantive scope is two small items (a keybinding decision
+ accelerator wire-up, a manual AT session) plus one already-scoped child
(#288). The issue nonetheless carries a `tier:feature` template with a
YAML machine block, a Mermaid dependency graph, four Global Invariants,
a Sequencing & Parallelism section, and an eight-item Re-planning
Protocol. The DoD's own last line — "Machine block, roster table, and
mermaid graph agree with reality at close" — is an admission that this
scaffolding can drift out of sync with the code and needs a manual
reconciliation pass to even close the issue. For a two-item residual,
that's meta-work whose cost is not obviously smaller than the delta it's
tracking. **Recommendation:** consider collapsing the surviving residual
directly into `#288`-style task issues and closing #75 now that the
`tier:feature` scope (H1/H2/a11y-names) has landed, rather than
maintaining a capstone-weight wrapper for two follow-ups.

### 5. (Low) Label hygiene

Labels include `bug` and `enhancement` together. Every concrete defect
this issue opened against (focus-follows-mouse, zero accelerators/
mnemonics) is fixed per the verified evidence above; what's left is a
keybinding-policy decision and a manual QA pass — enhancement/process
work, not a bug. Keeping `bug` risks mis-prioritizing it against actual
open defects elsewhere in the tracker. **Recommendation:** drop `bug`,
keep `enhancement`/`area:ux`/`tier:feature`.

## What's solid (no action needed)

- The scope boundary (§1) against #73, #76, #84, #288, and #59 is precise
  and, cross-checked against the fetched bodies of #59/#60/#288, does not
  actually overlap or contradict any of them.
- Global Invariant 2's "keep old stroke as alias where unambiguous"
  carve-out is applied consistently: the `viewValueStroke()` rebinding
  (mask+S→V) explicitly forgoes an alias *because* the old stroke would
  shadow Save, which is exactly the "unambiguous" exception the invariant
  names — not a hidden contradiction.
- The refutation record (matchJump vs. rotate binding, the "W=close+wire→E"
  scheme) is kept rather than silently dropped, and both are borne out by
  the current code (`WIRE_START` is plain W with no modifier, `CLOSE` is
  still mask+E) — good audit hygiene, worth keeping as the house style.

## Verdict rationale

Sound engineering content, verified line-for-line against the repository.
Downgraded from "sound" because the Definition of Done as written permits
the issue to close green while two of the three items named in its own
title remain undelivered, and one of those (Close's missing accelerator)
is a live discoverability gap the issue exists to prevent, blocked on a
decision nobody is actually blocked from making.
