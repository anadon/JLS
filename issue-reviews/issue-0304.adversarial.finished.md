# Issue #304: CAP-08: a published RV32 core JLS did not write imports, opens as a readable hierarchy, and executes its own firmware against the author's reference
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the issue

CAP-08 is a "capstone"-tier tracking issue in this repo's heavyweight AI-driven
planning system (YAML machine block, mermaid DAG, numbered ACs/KCs, a
Re-planning Protocol). It composes 14 required "feature" issues
(#314–#364, 113–175 maintainer-weeks by its own sum) plus 2 beneficial ones,
to make the importer realize the 14 cell types it currently validates but
does not map (`src/jls/hdl/yosys/CellValidator.java:58-68` vs
`src/jls/hdl/imp/NetlistImporter.java:234-259`), add hierarchy/subcircuit
import and export, add a four-state value core, and execute an imported RV32
core's own firmware against its own reference trace inside a wall-clock
budget. Six comments (2026-08-03 to 2026-08-08) already document three
rounds of self-correction (edge withdrawal, a maintainer ruling D13 that
struck a chunk of §2/§3/§5/Cost, and two "wrong evidence commit" notices).

## Findings, most severe first

**1. Half the issue's own motivating evidence does not exist on `master`.**
The Abstract/Impact section's claim that "hierarchy is refused in both
directions" cites `src/jls/hdl/HdlExporter.java:465` (`SubCircuit.class`
inside a `REJECTED` map declared at `:460`). I read the actual file at HEAD
(`e7731bd`, 1364 lines) — there is no `REJECTED` map and no `SubCircuit.class`
rejection anywhere in it. A comment on this very issue
(2026-08-03, "Evidence-pin notice") already flags this as branch-only code
from a to-be-deleted branch and warns "does not exist on master and never
did." The import-side half of the claim does check out
(`NetlistImporter.java:227-231`, confirmed present and matching), but a
reviewer opening this issue today and following the export-side citation
gets a dead pointer to code that was never merged. Recommendation: strip or
re-cite the export-side claim against what's actually on `master`, or
demote it to "proposed, tracked by #492" as the issue's own comment
instructs.

**2. The cost model and AC-4's budget rest on files that do not exist in
this checkout.** `docs/plan/REGISTRY.md` (source of every per-feature cost
band and the 113–175 mw sum) and `docs/machine-calibration.md` (source of
the 3.14×10⁶ events/s figure AC-4 and KC-08-1/KC-08-5 are derived from) are
both absent — `ls docs/plan` and a repo-wide search for
`machine-calibration.md` both come back empty. This is already
self-reported in the 2026-08-04 comment ("the docs/plan/ evidence tree...
is not present on the current default-branch head") but remains an
open, unaddressed contradiction four days later: the issue asserts numeric
acceptance/kill thresholds (10 minutes, 1.884×10⁹ events, a 2x regression
cap) that no artifact in the tree currently backs. Until those documents
exist or are re-homed, AC-4 and KC-08-1/KC-08-5 are not verifiable against
anything — they are asserted, not derived, contrary to what §4 claims
("Derivation, not assertion").

**3. AC-0 means the issue has no actual subject yet — it is a
specification for a specification.** The issue admits this outright: "As
originally filed this issue never named its target core... an acceptance
criterion whose subject is undetermined cannot be falsified." `test/fixtures/core-pin.properties`
does not exist (confirmed). The five selection criteria in §1 are
reasonable, but nothing forces convergence on one core — "PicoRV32-class"
is explicitly *not* pinned, which means the eventual choice, and therefore
whether AC-1 through AC-8 are even satisfiable for that specific core, is
entirely deferred to whoever executes FEAT-020. A capstone whose subject is
chosen after the acceptance criteria are written is a template, not a
falsifiable plan.

**4. Gameable acceptance criterion: AC-1's "committed manifest" is
self-certifying.** AC-1 requires the realized element counts to "match a
committed manifest" — but that manifest is produced from the same import
run being graded, with no independent oracle for what the counts *should*
be. A consistently-wrong mapper (e.g. one that silently maps `$dff` to the
wrong register variant) passes AC-1 as written, so long as it reports zero
unresolved problems and its own output matches its own committed manifest.
The real correctness check is deferred entirely to AC-3/AC-5 (execution
trace vs. reference, and the external-oracle differential) — AC-1 by itself
verifies plumbing, not correctness, despite reading as though it verifies
the import.

**5. Stale cross-reference not yet corrected in the body.** The Related
Issues section and `related` list still cite #59 ("HDL interoperability:
staged VHDL/Verilog support... It consumes #59's import stage rather than
re-filing it") as an active sibling. I confirmed #59 is `closed`,
`state_reason: not_planned`, closed 2026-08-03 — the day before this
issue's own 2026-08-04 comment flagged the same staleness. The comment
identifies the fix ("restate where #59's import stage now lives") but the
issue body has not been edited five days later.

**6. Unresolved internal contradiction left open past its own deadline
language.** The issue insists FEAT-019 (#321, Yosys JSON write) is
*required*, against `docs/plan/capstones/CAP-08-import-third-party-core.md:65`
which grades it *beneficial*, and a Completion Criteria line demands the
corpus doc be corrected. Given finding #2, that corpus doc does not exist
on `master` at all right now, so the "correct the document" resolution path
is currently impossible to execute — the contradiction can't even be fixed
as instructed until `docs/plan/` is restored or re-homed.

**7. Unbudgeted critical-path dependency.** The 2026-08-03 comment thread
found, then a maintainer ruling (D13, quoted verbatim) ordered *removed*
from this issue's body, the fact that FEAT-020 — "the spine" — is
`blocked_by: [339]` (FEAT-021, bidirectional ports), and #339 is not a
`requires_features` row. I independently confirmed #339 is open and does
declare `blocks: [320, 328, 360]`. D13's ruling ("record the prerequisite
and move on," no reconciliation required) is a legitimate scope-control
call, but the practical effect is that the capstone's true minimum cost
(113–175 mw) does not include unblocking its own critical path, and that's
by policy rather than oversight — worth flagging even though it's a known,
deliberate omission.

**8. Licensing screens the input but not the output.** Selection
criterion 2 requires an OSI license permitting redistribution of the
synthesized netlist as a committed fixture. Nothing addresses what license
terms attach to JLS's own *derivative* artifacts — the imported `.jls`
schematic, and especially AC-6's round-tripped netlist and any HDL a user
subsequently exports from it. If the pinned core is GPL/LGPL-licensed RTL
(PicoRV32 itself is ISC, but the issue does not commit to PicoRV32), a
committed derivative could carry copyleft obligations the project's own
GPLv3-avoidance discipline elsewhere (ELK/EPL-2.0, `ARCHITECTURE.md`
"Plugin trust boundary") does not currently address for this path.

**9. Scope size relative to stated maintenance model.** 113–175
maintainer-weeks (roughly 2.2–3.4 maintainer-years at 1 mw/week) for a
project `ARCHITECTURE.md` describes as a "single-maintainer pedagogy tool"
is an enormous ask. The issue never states whether this capstone is
actually intended to be funded soon or is purely long-horizon backlog
scaffolding — worth an explicit note given the gap between stated cost and
stated maintenance capacity.

## What's solid

- The dependency bookkeeping (14 required rows, 3-way count agreement
  between `requires_features`/table/mermaid graph, a DAG cycle walk across
  all 57 filed features) is unusually rigorous, and the 2026-08-03 edge
  audit correctly found and withdrew 8 mis-drawn ordering edges rather than
  leaving them stale.
- The five core-selection criteria (§1) are concrete and testable in
  principle, and "no silent mis-mapping" (KC-08-3, backed by a real
  committed test, `test/jls/hdl/imp/NetlistImporterTest.java:227`) is a
  sound engineering discipline worth holding under schedule pressure.
- The factual claims that do cite live code check out: `CellValidator`'s
  19-entry `SUPPORTED` set, the 5-type mapper switch plus constants, and
  `ElementRegistry`'s 35-entry vocabulary all match `src/` at HEAD exactly.
- The AC-4 budget arithmetic (600 s × 3.14×10⁶/s = 1.884×10⁹ events) and the
  Cost section's 113/175 sums are internally correct, once you accept their
  now-missing source documents.

## Recommendation

Do not start execution work against this issue as currently worded.
First: (a) re-verify and re-cite the HdlExporter export-refusal claim
against `master`, or drop it; (b) resolve where `docs/plan/REGISTRY.md`
and `docs/machine-calibration.md` now live (or restate AC-4/Cost against
whatever replaces them) before treating any AC-4/KC-08 number as binding;
(c) fix the stale #59 reference; (d) before FEAT-020 work begins, pin an
actual core in `test/fixtures/core-pin.properties` so AC-0/AC-1 have a
falsifiable subject rather than a five-criterion template.
