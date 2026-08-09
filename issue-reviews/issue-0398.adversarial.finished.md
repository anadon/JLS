# Issue #398: TASK-0078: a clock stops being an ordinary wire — a Clocked capability, declared domains, and every unsynchronised crossing reported by name
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The diagnosis is real — `Register.currentC`, `StateMachine.oldClock`, `Memory.lastClock`
and `RegisterFile.currentC` are four hand-rolled edge detectors (confirmed at
`src/jls/elem/Register.java:698`, `StateMachine.java:657`, `Memory.java:996`,
`RegisterFile.java:451`), the clock pin really is `ins.get(1)` magic in
`HdlExporter` (confirmed, see finding 3), and `Timed` (`src/jls/elem/Timed.java:25`)
really is the idiom to copy. But this "task"-tier issue bundles a capability
interface, a saved-attribute change, and an entire clock-domain-crossing static
analyzer — including synchroniser pattern matching and a false-positive-rate
research question the issue admits it cannot threshold in advance — into one
unit of work, one of its own load-bearing citations points at a pin name its
sibling task never created, and its own "blocks execution" open questions are
left unresolved while the Method section proceeds as if ready to implement.

## Findings, most severe first

### 1. The reset-crossing rule cites a pin name TASK-0077 never creates

§1 (Abstract) and §12 (Related Work) both assert this task's cross-domain
reset rule "reads TASK-0077's `CLR`/`PRE` pin." TASK-0077 is now filed and
open as #478. Its actual interface (§7.4): *"`Register.init(...)` gains an
`R` input, appended last in all four orientation arms... created only when
`reset != none`"* — one pin, named `R`, gated by a `reset ∈ {none,sync,async}`
attribute. There is no `CLR`, no `PRE`, and no separate async/sync pin pair
anywhere in #478's text. #398's own reset-crossing rule (§7.10, "a reset net
whose driver's domain differs from its sink's is itself a crossing", P7) is
written against a two-pin (clear/preset) model that the actual sibling task
does not build. Either #398 was drafted against an earlier, different design
for #478 and was never reconciled after #478 was filed, or the two issues
disagree about what the reset pin is called and how many there are.
**Recommendation:** fix §1/§12 to read `R` (singular) before any
implementation reads this citation literally; re-verify §7.10's crossing rule
still parses against a single gated pin rather than a two-pin clear/preset
scheme.

### 2. The dependency graph has a half-edge: #478 claims to block #398, #398 doesn't record it

#398's machine block declares `blocked_by: [336]` only. But #478
(TASK-0077, filed same day) declares `blocks: [327, 398]` — i.e. #478 itself
asserts it must land before #398. #398's own prose acknowledges the gap and
promises to fix it: *"Unfiled prerequisite... A link pass adds it. No other
task-to-task edge is claimed."* That promise is unmet: #478 has been open and
filed since 2026-08-03, and as of this review #398's `blocked_by` still reads
`[336]`. (The same gap was independently flagged in this fleet's review of the
parent issue #327, finding 4, `issue-reviews/issue-0327.adversarial.md`,
confirming it is not a stale observation but a persisting one.) Concretely,
nothing in the tracker today stops an agent from picking up #398 before #478
lands, which the issue's own §Status text calls "necessity, not convention."
**Recommendation:** add `478` to `blocked_by` now; it is a one-line edit the
issue already flagged as owed four days before this review and still hasn't
made.

### 3. Evidence commit is unreachable from this checkout; the repo owner's own repin comment must be applied by hand throughout

`evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` does not resolve
(`git log` confirms; `HEAD` here is `53116252116b9e74bbdf64d7df1f5e08b4e1768b`).
The issue's own comment (`issuecomment-5171449489`, posted same day) discloses
this and gives corrected line numbers for `master`: `HdlExporter.java:547` →
`:489`, `:640` → `:582`. Spot-checked against current `HEAD`: both hold —
`:489` is `if (el instanceof InputPin || el instanceof Clock) {` and `:582`
is `HdlModel.Operand clock = operand(ins.get(1), nets, groups);`. So the
substance is sound, but every citation in the body still carries the stale
line numbers, and the comment also states that `docs/plan/**`,
`docs/machine-calibration.md`, `docs/parity-contract.md` and
`docs/virtual-hardware-parity.md` (cited elsewhere in this task family, e.g.
by #478) don't exist on `master` at all. #398 itself doesn't cite those paths,
but its `docs/batch-interface.md:33-48` and
`docs/simulation-semantics.md:362-370` citations were independently verified
here and do hold at the stated lines. A closer must re-derive every citation
by content, not by line number, exactly as the issue's own rule 6 demands —
but the body hasn't been edited to do that yet.
**Recommendation:** re-pin the evidence commit or explicitly note "see
issuecomment-5171449489 for `master`-relative line numbers" inline in §2.

### 4. Two "Blocks execution" open questions are left unresolved while §8's Method proceeds as if ready to start

Open Question 1 (where the domain declaration lives) and Open Question 5
(whether `phase` applies to `LEVEL_HIGH` sinks) are both marked "**Blocks
execution.**" Neither is answered in the issue text — each states a
"recommended default" and stops. §8's Method checklist does not gate on
resolving either before its first substantive step ("Add `jls.elem.Clocked`
in the `Timed` idiom... implemented by `Register`, `StateMachine`,
`RegisterFile` and sync-write `Memory`"). Worse, OQ1's very premise is
under-specified against the issue's own §7.10: the domain-assignment formula
assumes "top-level `InputPin`s carrying a declared clock role" as one of two
root kinds, but no such concept exists anywhere in the current tree (`grep -n
"clock role" src/jls/elem/*.java` — no output) and §7.1 ("External interfaces
modified") never lists an attribute, dialog control, or save-format addition
that would let a user declare one. The data-transformation math in §7.10 is
therefore already committed to input data (a "declared clock role") that no
other section of the same issue says how to create, and OQ1 — the question
that would settle this — is left open and merely "blocks execution" in
principle, not in the plan.
**Recommendation:** either resolve OQ1 and OQ5 in the issue body before
picking this up, or add an explicit checklist item to §8 that makes their
resolution step zero; and add the missing interface for declaring a
top-level `InputPin`'s clock role, since §7.10 already depends on it existing.

### 5. Scope: a "tier:task" issue is asked to build a small CDC static analyzer, and the project's own roadmap doc treats that as a hard, feature-scale problem

`docs/capability-roadmap/lf-08-clocks-and-cdc.md` (part of this same repo)
bands "structural-only" CDC work at 8-11 maintainer-weeks even at its
described floor, and separately states real commercial CDC tools are a
"mature, expensive product category" (Siemens Questa CDC, Synopsys VC
SpyGlass CDC, Cadence Conformal/Jasper CDC) and that "JLS must never claim
CDC sign-off." #398 asks a single task to: add the `Clocked` capability
(reasonable, narrow); add a validated `phase` attribute (reasonable, narrow);
**and** build `jls.timing` from scratch — domain inference over a shared
partition, a four-way derivation classifier (`GATED`/`GENERATED`/`MUXED`/
`UNDRIVEN`), a two-flop synchroniser pattern matcher with a fan-out
condition, a reset-crossing rule, `UNSAFE_BY_CONSTRUCTION` multi-bit
classification, HDL IR domain carriage, a new `-cdc` batch surface, a
permutation-purity test, and a published corpus false-positive rate with no
agreed threshold (H4 / Open Question 4 admit this explicitly: *"H4 has no
threshold yet, and without one 'the check is usable' is unfalsifiable"*).
That is the shape of a small feature on its own, not a task riding alongside
two much smaller, genuinely task-sized changes (`Clocked`, `phase`). The
issue's own Completion Criteria (16 checkboxes) and Predictions (P1-P10, each
multi-part) are consistent with a feature-sized unit of work being tracked
under `tier: task`.
**Recommendation:** split `jls.timing`'s domain-inference/crossing-check work
into its own `tier: task` or `tier: feature` issue, distinct from the
`Clocked`-capability-plus-`phase` change; landing the capability and the
`phase` attribute does not require the CDC analyzer to exist in the same
commit, and bundling them raises the cost of any one piece being blocked or
falsified (per H4) to blocking all of them.

### 6. Transitive blocking chain is deeper than #398's own text discloses

#398 declares `blocked_by: [336]` and frames #336 as "the shared net
partition... the reason for the blocked_by." But #336 (FEAT-004) itself
declares `blocked_by: [315]` (FEAT-001, registry-keyed table totality),
and #315's own review material records it as not yet started ("two 'not
filed' planned tasks... with no child issues yet," per this fleet's review of
#336). So the real critical path to #398 becoming startable is
315 → 336 → 398, not just 336 → 398. Nothing in #398 is factually wrong here
(it correctly names its one direct blocker), but a reader relying on #398's
own dependency section to gauge readiness would materially underestimate how
far off execution is, since two full unstarted upstream units of work sit
between "today" and "this task can begin."
**Recommendation:** note the transitive chain in §Status, or at minimum link
to #336's own `blocked_by` so a scheduler doesn't have to walk the graph by
hand.

### 7. `RegisterFile`'s clock-pin claim is asserted, not evidenced, and is the one sink most likely to refute H1

O4 asserts "`RegisterFile`'s `C` is appended after a variable number of
RA/WA/WD/WE pins, so an index is not even expressible" and H1's falsification
path treats "any sink's clock is not name-addressable" as the risk to watch
for. No citation (file:line) is given for `RegisterFile`'s pin layout the way
every other observation in §2 gets one — a `Register.java:230-231` code
excerpt but nothing analogous for `RegisterFile`. `RegisterFile.java:451`
(`private int currentC;`) confirms the field exists, but the issue never
shows the reader `RegisterFile.init`'s pin-naming to substantiate "an index
is not even expressible," making O4's strongest claim (the one that most
directly supports needing name-based lookup rather than a simpler
index-based fix) the least evidenced observation in the whole section.
**Recommendation:** add the `RegisterFile.init` excerpt showing the named
`C` pin, the same evidentiary standard §2's other observations meet.

## What's solid (one line each)

- The four duplicate edge-detector fields are real and correctly cited
  (`Register.currentC`, `StateMachine.oldClock`, `Memory.lastClock`,
  `RegisterFile.currentC`), confirmed at current `HEAD`.
- The `Timed` idiom really is capability-interface precedent worth copying;
  `Timed.java:25`'s javadoc matches the issue's characterization exactly.
- The `HdlExporter` clock-pin magic index (`ins.get(1)`) is real, and O5's
  claim that `HdlExporter.java`'s port-vs-clock branch must stay unchanged
  while domain annotation is added alongside it is a sound, narrow
  compatibility constraint.
- O7's silent-drop hazard for a `phase`-shifted clock loaded by an older
  reader is a genuine, correctly-reasoned risk given the documented
  no-format-bump discipline for new attributes on existing element types.
- The `jls.timing` package's stated headless/no-new-threads constraints
  (§7.9) are consistent with the project's existing `jls.sim`
  headless-by-construction discipline (`ARCHITECTURE.md`'s description of
  `HeadlessCoreRatchetTest`), and the per-package JaCoCo "born floored"
  pattern cited in §6 matches the actual `pom.xml` rules for `jls.sim`,
  `jls.elem`, and `jls.collab.op` (verified at `pom.xml:355-499`).
- The refusal to let a global off-switch suppress crossings (H4's
  falsification response, §7.11's failure-mode table) is a defensible
  design stance against exactly the failure mode the project's own CDC
  roadmap doc names as the top risk.

## Verdict rationale

Not `should-not-proceed`: the underlying technical need is real and most of
the interface design is careful. Not `sound-with-concerns`: finding 1 is a
factual contradiction between this issue's own text and its sibling's actual
filed contract that would mislead an implementer building the reset-crossing
rule, finding 2 is a broken half-edge in the dependency graph that the issue
itself promised to fix and hasn't, and finding 4 shows the issue proceeding
into an implementation checklist while its own "blocks execution" gates sit
unresolved and, in OQ1's case, underspecified at the interface level. Those
are not polish items — they are the kind of inconsistency that produces
either a stalled implementation or a `REPLAN:` shortly after work starts. Fix
findings 1, 2, and 4 before assignment; finding 5 (scope) should be resolved
by splitting the issue rather than trimmed in place, since the `jls.timing`
analyzer is the majority of the risk and cost here.
