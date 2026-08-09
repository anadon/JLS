# Issue #459: TASK-0083: the balanced-ternary machine exists as drawn boundaries against a stated census, with the ternary ALU and the three-way branch green against the emulator
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what was checked

Fetched issue #459 (open) and its one comment; fetched #412 (hard `blocked_by`) and #493 (the
tracker's own evidence-pin audit) for cross-reference. Verified in the working tree at HEAD
(`5b05d67`, current `master`): `src/jls/hdl/HdlExporter.java`, `src/jls/elem/RegisterFile.java`,
`src/jls/elem/Memory.java`; confirmed absence of `machines/`, `BRIEF.md`, `docs/machine-calibration.md`,
`docs/parity-contract.md`, `docs/virtual-hardware-parity.md` anywhere in the repo; confirmed the
evidence commit `2d0ca9d` does not exist locally.

## Findings, most severe first

**1. [Critical] The quantitative core of the design (H2, the register-file decision) rests on
documents that do not exist on `master` and are stated by the tracker's own audit to be
unrecoverable.** O4's table (`+6.94` vs `+114.53` ev/cycle) and O7's calibration constants
(`R`, `k`, `α`) are cited to `docs/machine-calibration.md`. That file does not exist anywhere in
this repository (`find` over the whole tree returns nothing), nor does `docs/parity-contract.md`,
`docs/virtual-hardware-parity.md`, or `BRIEF.md` (cited for decisions D5/D10). Issue #493 — filed by
the same author specifically to audit this — classifies exactly these paths under "Absent from
`master` entirely… **Unrecoverable by re-reading**. These files never existed on `master`… the
quoted text in the issue body is the only surviving copy." That means the entire justification for
"one `RegisterFile` instead of 27 registers" — the single most consequential architecture decision
in this task, enshrined as a Definition-of-Done line item ("The `regfile` boundary contains exactly
one `RegisterFile` and zero `Register` elements") — cannot be independently checked by anyone
picking this issue up. If the `+114.53` figure was mistyped, misattributed to the wrong fixture, or
simply invented under evidence-commit pressure, there is no way to catch it. **Recommendation:**
before this task is actionable, either recover/recreate `docs/machine-calibration.md` on `master`
with its own evidence trail, or restate H2/O4 as an unverified assumption pending remeasurement, not
as settled fact driving a hard Definition-of-Done requirement.

**2. [High] O3's HDL-refusal evidence is already conceded wrong by the issue's own comment and by
#493 — and the review confirms the underlying code shape has changed.** The maintainer's bot comment
on this issue (2026-08-03) states `HdlExporter.java:460-477`'s `REJECTED = Map.of(...)` block (the
per-class refusal *reasons*) is branch-only and "will land on unrelated code" if re-read at `master`.
I confirmed this directly: on `master`, `HdlExporter` classifies elements into three sets —
`EXPORTED` (`:422`), `SKIPPED` (`:431`), `TOPOLOGY` (`:436`) — with no fourth `REJECTED` bucket and no
parenthesized reason strings; an unclassified element (which `Memory`, `RegisterFile`, `SubCircuit`
and `FieldExtend` all still are, per `EXPORTED`'s member list at `:422-427`) is refused with a bare
`describe(el)` name, no reason. So while the *substance* of O2 (`buildModel` refuses a circuit with
`RegisterFile`/`Memory`) is still true today, the specific text this task's own Completion Criteria
requires — "`CENSUS.md` states that this machine cannot be exported to HDL, **naming the refused
element classes and their reasons**" — cannot be sourced from the exporter as it exists on `master`,
because master's exporter carries no reasons to name. **Recommendation:** either land #492 (which
#493 says owns the `REJECTED` bucket) first, or rewrite the `CENSUS.md` obligation to not promise
reason-text the exporter doesn't currently produce.

**3. [High] Two of three hard blockers have no issue number and are absent from the machine-readable
`blocked_by` field.** `blocked_by: [412]` in the YAML frontmatter names only #412. The prose admits
TASK-0061 (N-ary element family) and TASK-0082 (reference emulator/assembler) are also hard
blockers — TASK-0082 explicitly "with no fallback: there is no other counterparty for a custom
ISA" — but "Neither is written in the machine block because both are being filed concurrently and
their numbers cannot be verified here. **A link pass must add them.**" That link pass has no owner,
no issue number, and no deadline anywhere in this issue. Any tooling, dashboard, or reviewer that
trusts the `blocked_by` field (as the sweep evidently does elsewhere) will see this task as blocked
only by #412 and will not see the emulator dependency at all. A task cannot be usefully picked up
while two of its three prerequisites are simultaneously "hard" and "unaddressable in the tracker."
**Recommendation:** do not mark this task ready until TASK-0061 and TASK-0082 exist as filed issues
and `blocked_by` is updated to name them.

**4. [Medium-High] The acceptance gate for the headline metric (P1/P7, the census and the
events-per-instruction band) is self-referential.** P1 is "`machines/cpu-t3/CENSUS.md` states a
per-boundary element count and the drawn boundaries match it" — but `CENSUS.md` is written by the
same person doing the drawing, in the same PR ("Write `CENSUS.md` **first**"). The test enforces
`declared == measured`, not `measured ∈ [target band]` against anything external. Combined with a
declared band of 450–950 against a target of ~620–760 (roughly ±35% either way), an implementer can
draw a bloated `talu` boundary, write a `CENSUS.md` row that matches it, and the test is green. The
"wide band on purpose… still catches a boundary that doubled" defense in §11 doesn't hold for the
common failure mode: one boundary growing 40–60% while the total stays inside 450–950 because other
boundaries are smaller than planned. There is no boundary-level ceiling, only a whole-machine one.
**Recommendation:** add a per-boundary band (even a loose one, e.g. ±50% of each row's own target),
not only an aggregate one.

**5. [Medium] H3 (two ALUs) is asserted as a testable, falsifiable design claim but no prediction
(P1–P10) actually tests it.** §11 even flags this itself: "Two ALUs looks like an accident… H3
states it as a design claim with a falsification path, which is the only honest way to carry it" —
but §10's stated falsification move ("delete the second ALU… a smaller machine that teaches the same
thing is strictly better") requires someone to actually *attempt* covering the ISA's binary ops with
one ternary ALU and fail, and no Prediction or Data-Collection entry does that. As written, H3 is
decorative rigor: it has the shape of a falsifiable hypothesis without a corresponding test, so the
task can close with two ALUs drawn and H3 "unrefuted" by default rather than by evidence.
**Recommendation:** either add a prediction that exercises the binary-ALU-coverage question, or drop
H3's falsifiability framing and state it plainly as a design decision.

**6. [Medium] Scope: 7 of 9 required `.jls` boundaries have no correctness gate.** Materials &
Apparatus lists nine files to build (`talu.jls`, `balu.jls`, `regfile.jls`, `decode.jls`,
`loadstore.jls`, `br3.jls`, `packunpack.jls`, `cpu.jls`, `t3-soc.jls`). Only two — `talu` and `br3` —
have bring-up predictions (P3–P6) or a BRINGUP.md-required "green" row. The rest need only exist and
contribute to the aggregate census sum (finding 4). A `decode.jls` or `cpu.jls` that is wired
incorrectly, or that is a near-empty stub padded to hit its census row, satisfies every stated
Prediction and every Completion Criterion. This is a large amount of unverified surface area bundled
under a task titled around "green against the emulator." **Recommendation:** either scope the
deliverable list down to the two bring-up boundaries plus stubs explicitly marked out-of-scope for
correctness, or add at least a construction/connectivity smoke check for the other seven.

**7. [Low] `blocked_by` chain depth risk (feasibility, not a defect in this issue specifically).**
#412 (this task's one *named* blocker) is itself blocked by #382, which the #412 body says has not
landed, and #412's Related Work further names #170 and #78 as open dependencies its own vocabulary
depends on. Filing #459 as `tier: task` (i.e., presented as near-term, actionable work) while sitting
three-plus issues deep behind open prerequisites — two of which aren't even filed yet (finding 3) —
overstates how actionable this is today.

## What's solid (brief)

- H1 and O5/O6 (BET word fits the existing 32-bit dense-storage path with zero format change) check
  out: `RegisterFile.java:140-154` and `Memory.java:1224,1234` match the issue's citations exactly on
  current `master`, despite the evidence-commit pin being stale.
- The "r0 reads zero" convention (Open Question 5) is real: `RegisterFile.java` has a documented
  `reg0zero` attribute and convention, so that decision is implementable as described.
- P7's "count events, not wall-clock time" methodology is a defensible response to the admitted
  unmeasured `α` — good scientific hygiene, independent of whether the source numbers are trustworthy.
- "Do not couple bring-up to #232 (hot-path fix)" is sound scoping discipline.
- No security, licensing, or backward-compatibility hazard: no format bump, no new dependency, no
  CLI/API surface — this part of the interface contract is honestly stated and low-risk.
