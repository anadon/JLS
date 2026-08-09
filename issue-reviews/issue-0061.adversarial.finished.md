# Issue #61: HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists (restricted cell pipeline)
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of what's being attacked

#61 is a large, template-driven "feature" tracking issue for Yosys-JSON-based
Verilog import. The core technical bet (delegate parsing/elaboration to an
external Yosys, map a restricted ~15-cell netlist onto JLS elements) is
well-researched and the landed code (`jls.hdl.yosys`, `jls.hdl.imp`) matches
what the issue claims. The problems here are almost entirely in the issue's
own bookkeeping layer — a 15-comment, multi-pass "REPLAN"/absorption history
that has left the machine-readable header stale, dependency edges
unmirrored, and the task roster genuinely ambiguous to a reader who (like
this review) only has the GitHub API, not tribal knowledge of the comment
thread.

## Findings, most severe first

### 1. Unmirrored `blocked_by` edge: #61 → #62 exists, #62 → #61 does not (CONFIRMED)

The issue body's machine block declares:
```
blocked_by: [60, 62]
```
i.e. #62 (auto-layout) is claimed as a hard prerequisite. But #62's own
machine block (fetched directly) declares:
```
blocked_by: []
blocks: []
```
No edge back to #61 exists on #62's side at all. This is precisely the
"half-edge defect" this repo's own convention names and warns against — see
the 2026-08-08 comment on this very issue withdrawing #291's unmirrored
`blocked_by: [61]` for exactly this reason ("an edge that is removed on one
endpoint and left standing on the other is the half-edge defect the
link-pass discipline exists to prevent"). #61 itself is currently guilty of
the mirror-image bug: an edge asserted here that #62 does not corroborate.
Compounding this, #61's own prose elsewhere hedges the edge into near-
nonexistence: "62 gates integration close-out only ... not mapper work,"
which is inconsistent with treating #62 as a blocking prerequisite in the
YAML at all.

**Recommendation:** either add `blocks: [61]` to #62 (if the dependency is
real and blocks the whole feature) or downgrade #61's `blocked_by` entry to
`related` and state explicitly, once, what #62 actually gates (the import
UI/close-out, not the mapper) — the body already says this in prose but the
machine block contradicts it.

### 2. `blocked_by` is missing #339 despite an explicit, unactioned self-instruction to add it (CONFIRMED)

The 2026-08-04 comment thread on this issue (the #320 absorption, part B)
states in writing: "`blocked_by` gains **#339** (from #320, with
`NetlistImporter.java:185-189` as its reason)." That never happened — the
body's machine block still reads `blocked_by: [60, 62]`. Checking #339 (the
bidirectional-ports feature) confirms the mirror is also missing there: its
`blocks` list is `[320, 328, 360]` — still pointing at **#320, which is
closed as a duplicate of this issue** — never repointed to #61, despite the
same comment thread flagging this exact repointing as a "mirror
obligation ... not done here." Two dangling TODOs, four days old at time of
review, neither executed.

**Recommendation:** either execute the repoint (add 339 to #61's
`blocked_by`, add 61 to #339's `blocks`, remove the stale 320 reference) or
explicitly retract the 2026-08-04 comment's claim if #339 turns out not to
gate this issue after all.

### 3. Task roster is unverifiable through normal GitHub tooling — `has_children` is false while comments claim two active child tasks (CONFIRMED)

`issue_read(get)` on #61 returns `has_children: false`, and
`get_sub_issues` returns an **empty list**. Yet the two most recent comments
(2026-08-04 "Pass 2" and 2026-08-08 "ROSTER CORRECTION") both assert that
#448 (TASK-0047) and #449 (TASK-0048) are "this issue's task set," having
been re-homed from the closed #320 via a bespoke `part_of_feature: 61` YAML
field inside each task's own body — not GitHub's native issue-hierarchy
feature. I confirmed by fetching #448 and #449 directly: both declare
`part_of_feature: 320` in `Status & Dependencies` — i.e. **the field itself
was never actually edited** despite the 2026-08-08 comment claiming "Both
have been given the matching `part_of_feature: 61` correction on their own
issues." That correction is asserted in a comment but is not present in
either target issue's body.

This means: (a) the issue's own `requires_tasks: []` / "no children filed
yet" in its machine block is stale and contradicts the comment thread's
claimed roster; (b) the claimed roster itself is *also* stale/unexecuted
where it lives (the tasks' own bodies); (c) anyone auditing scope via the
API — which is the only tool this review has, and plausibly the only tool
any automated agent picking up this issue will have — sees zero linked
children and would have no way to discover #448/#449 without reading all 15
comments end to end.

**Recommendation:** Either use GitHub's actual sub-issue linking
(`sub_issue_write`) so `has_children`/`get_sub_issues` reflect reality, or
at minimum actually edit #448 and #449's `part_of_feature:` fields as the
2026-08-08 comment claims was done, and update #61's own `requires_tasks`
from `[]` to `[448, 449]`. As filed, the roster is a claim made only in
prose that the structured data flatly contradicts.

### 4. Task roster leaves three of the five `planned_tasks` items unaccounted for after the #320 absorption (PLAUSIBLE)

#61's body lists five `planned_tasks`: subprocess runner, `jls_map.v`
completion, mapper completion, import UI, parity suites. #448 covers flat
cell realization (part of "mapper completion"); #449 covers hierarchy (also
part of "mapper completion," per #449's own text: "TASK-0048 owns
hierarchy, this issue owns the flat cell set"). Neither says anything about
the subprocess runner, `jls_map.v` completion, the import UI, or the parity
suites. The 2026-08-08 "ROSTER CORRECTION" comment describes #448+#449 as
discharging "this issue's §7 'Cell→element mapper for the restricted
set'" — true as far as it goes — but never states whether the other three
`planned_tasks` entries remain owned directly by #61, are still unfiled, or
were silently dropped somewhere in the #320/#448/#449 churn. Given how much
churn this issue has already absorbed (a full sibling feature #320, task
migrations, ledger amendments), a reader cannot currently tell whether the
subprocess runner and import UI are still live scope or fell out of the
shuffle.

**Recommendation:** a REPLAN comment (or a body edit) that explicitly
re-confirms all five `planned_tasks` entries are still open and lists which
issue number, if any, now owns each one.

### 5. Abstract's feasibility framing ("resolved favorably ... every surviving cell has a direct or ≤4-element realization") is true of the *design*, not of the *current implementation*, and the gap between the two produces a live, shipped defect (CONFIRMED, evidence from code)

Verified directly against `src/jls/hdl/imp/NetlistImporter.java:234-258`:
the mapper's `switch (type)` has cases for exactly five cell types (`$not`,
`$and`, `$or`, `$xor`, `$mux`). Verified against
`src/jls/hdl/yosys/CellValidator.java:58-68`: the validator's `SUPPORTED`
set has nineteen cell types (plus both memory forms, plus hierarchy
instances). That means fourteen validator-accepted cell types —
`$add`, `$dff`, `$dlatch`, `$tribuf`, `$bmux`, `$pos`, five reduction/logic
ops, plus `$mem`/`$mem_v2` — currently pass `CellValidator` and then throw
`ImportException` from `NetlistImporter`. Two shipped tests
(`NetlistImporterTest.unrealizedButValidCellIsRejectedNotMismapped`,
`syncWriteRamValidatesButIsNotYetRealized`) currently assert this
"validates, then refused" behavior and pass — which is itself the state
#448 (correctly) calls "a state a user can reach" and treats as a
crash-equivalent UX bug, not a feature. The Abstract's plain-language
promise — "a student pastes ... Verilog ... and gets an editable,
simulatable JLS circuit ... or a one-pass, source-located, teachable
rejection" — undersells how far short of that the current increment is:
a design containing e.g. a single register or adder is not teachably
rejected at the *design* level (it was never going to import in the first
place, full stop, since Register/Adder/Memory/TriState mapping don't exist
yet); it is rejected only after the validator tells the user their design
is fine.

This is not a defect in the *plan* (§2's "why these cuts" honestly
describes the front-end/mapper split as intentional and phased), but the
Abstract oversells present capability relative to the roadmap sections
further down, which are more candid. A reader skimming only the Abstract
would reasonably expect substantially more than five realized cell types
exist today.

**Recommendation:** soften the Abstract's "Feasibility was resolved
favorably" framing to make clear that favorable feasibility is a *design*
finding (true, and well-evidenced in §7.2 of the research doc, which this
review also verified), while realization coverage is 5/19 cell types at
time of writing — that number belongs in the Abstract or the Status
section, not buried three re-derivations deep in comment #448.

### 6. Open Question 1 ($adff / Register async-clear) has a circular data dependency across three issues (PLAUSIBLE)

Ledger entry 6 and Open Question 1 both say the `$adff` reject-vs-realize
decision is "pending real corpus reject-frequency data." The 2026-08-08
roster-correction comment says that number is computed by #448's corpus run
and "arrives here" (i.e. gets reported back to #61). But #448 itself is
scoped only to realizing the 19 *currently-supported* cell types — `$adff`
is explicitly *not* in `CellValidator.SUPPORTED` (it's in the separate
reject-with-message table), so nothing in #448's corpus construction
obviously produces `$adff` reject-frequency statistics unless #448's corpus
authors go out of their way to also seed rejected designs. #448's own body
does list this as one of its Open Questions ("Where does the `$adff`
reject-frequency number get recorded? ... Recommended default: a comment
on #61") — so the intent is there — but nothing enforces that #448 actually
measures it, and #61's own P5/parity-suites planned task (the seeded-reject
corpus) looks like the more natural owner of that measurement. There's a
real risk this number never gets produced because two different tasks each
plausibly-but-not-definitely own it.

**Recommendation:** name one task explicitly as the owner of the `$adff`
frequency measurement (probably the "parity and round-trip suites" planned
task, since it already owns "seeded-reject corpus with source locations
(P5)"), not "whichever of #448 or the eventual parity suite gets there
first."

### 7. Version floor and CI-arming claims — spot-checked, hold up

`YosysVersion.java` 0.38 floor and `#266`'s yosys-armed CI legs are cited
with file:line references; I did not re-derive CI configuration but the
code-level claim (0.38 floor exists in `YosysVersion.java`) is consistent
with what the issue and #199 comment describe. Not independently verified
against `.github/workflows/`, flagged only as unverified-but-plausible.

## What's solid (no rework needed)

- The core architectural bet — delegate to Yosys, consume `write_json`,
  restrict to a legalizable cell set, refuse loudly outside it — is
  well-evidenced by `docs/hdl-support-research.md` §7.2, which I read in
  full; the "~15 surviving cell types, direct or ≤4-element realization"
  claim is stated there, not invented for this issue.
- Global Invariant 4 ("Rejection is total or import is total: no
  half-imported circuits, ever") is actually enforced in the landed code —
  `NetlistImporter.importNetlist` collects problems and throws before
  layout, confirmed by reading the mapper's structure. This is a real,
  checked guarantee, not aspirational prose.
- The `jls_map.v` "landed prior work" claims (PR #196, `$xnor`/`$eq`/`$ne`
  rules, `ImportPipelineTest.techmapLegalizesXnorToTheSupportedSet`) are
  accurate: I confirmed the rules exist at the cited lines and the test
  exists and is yosys-gated (skips cleanly without a toolchain).
- The GPLv3/EPL-2.0 licensing rationale for keeping Yosys/ELK
  out-of-process (cited in ARCHITECTURE.md's "Plugin trust boundary"
  section and #62's decision history) is coherent and consistently applied
  across #61, #62, #63.
- `evidence_commit: 29afb26` is honest: zero commits touch
  `src/jls/hdl/`, `test/jls/hdl/`, or `docs/hdl-support-research.md`
  between that commit and current HEAD, so none of the code-level claims
  in the body have silently drifted.

## Bottom line

The engineering design is sound and the individual code claims check out
against the actual repository. The issue is marked "sound-with-concerns"
rather than "needs-rework" because none of the findings above block useful
work from starting on the mapper (#448/#449 are independently well-specified
regardless of whether their `part_of_feature` field points at #61 or the
dead #320) — but the bookkeeping debt (unmirrored edges, an invisible task
roster, three of five `planned_tasks` unaccounted for, an Abstract that
oversells current coverage) is exactly the kind of thing that will bite the
next agent or maintainer who trusts the machine block or the API instead of
reading all fifteen comments, and it should be swept before this issue
accumulates a sixteenth REPLAN.
