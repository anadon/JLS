# Issue #392: TASK-0079: the RV32 machine exists as drawn boundaries with a tested element census, and its ALU and register file are green against an independent reference
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The core method (a per-boundary census enforced as an equality budget, a
bring-up ledger whose green rows must name an existing test) is a genuinely
good idea and is specified precisely (§7.10, P3, P7). But the issue as filed
is not actually startable, its own dependency metadata is already stale by
its own comment thread, and several of its cited evidentiary anchors do not
resolve in this checkout. Ranked findings follow.

## Findings

### 1. [High] Three hard prerequisites are unnamed, unfiled issues — the task cannot be worked as written
The body lists `blocked_by: [389]` in YAML, but §"Status & Dependencies"
prose then adds: *"Three further task-to-task edges are in scope and cannot
yet be named, because those tasks are being filed concurrently."*
TASK-0038 (construction verbs), TASK-0070 (`jls.mach` reference), and
TASK-0076 (byte lanes) have no issue numbers. The Method section is not
merely informed by these — it is *gated* on them: "generate bus fan-out, the
decode table and the CSR file through TASK-0038's verbs... **Nothing in this
task emits `.jls` text and re-parses it**," and O6 says the only existing
alternative construction path "bypasses the `CircuitOp` layer... Use it for
nothing that ships." So `decode.jls` and `csr.jls` are literally
unauthorable under this issue's own constraints until an issue that doesn't
exist yet lands. A `blocked_by` field that omits three of four real blockers
because they lack numbers is not a dependency graph a scheduler or a
contributor can act on today.
**Recommendation:** do not mark this ready-to-pick-up until TASK-0038 and
TASK-0076 exist and are linked; the mermaid diagram's "edge in scope, number
added by the link pass" placeholders should block the `enhancement` label
being actionable at all.

### 2. [High] The issue's own machine-readable metadata is already wrong, and the DoD points at a closed issue
Three comments (2026-08-03, 2026-08-04, 2026-08-08) progressively correct
`part_of_feature: 326` → #202, because #326 (FEAT-038) "was closed as a
duplicate" — twice re-confirmed, most recently the same day this review runs
(2026-08-08). Per the second comment: *"No body or title was edited —
migration is by comment."* The body's own Definition of Done still says:
*"Landing reported on #326 with a `STATUS:` comment, and on #325 as the
consumer."* Followed literally, a contributor posts a landing report to a
**closed, duplicate-of-#202** issue. The §"Threats to Validity" cost banding
("FEAT-038's band is 12-26 maintainer-weeks") also cites the now-closed
feature for its scope justification.
**Recommendation:** edit the body (not just append another comment) to fix
`part_of_feature`, the DoD's landing-report target, and the cost citation
before this is worked — three unresolved comment-only patches is a
correction debt future readers will not reliably discover.

### 3. [Med-High] A load-bearing evidentiary anchor does not exist on the target branch
Comment #1 states `evidence_commit: 2d0ca9d` "exists only on a branch that
will not be merged," and explicitly flags `src/jls/hdl/HdlExporter.java:469-477`
(the quoted `RegisterFile.class`/`FieldExtend.class` HDL-refusal list backing
O3) as branch-only code "on `master` it is the work described by #492... not
present." I verified directly in this checkout: `src/jls/hdl/HdlExporter.java`
(1364 lines) contains no `RegisterFile`/`FieldExtend` refusal strings and no
"does not support yet" text anywhere — the O3 evidence as quoted is not
reproducible here. Yet §7.12 claim 5 and the DoD both still require
`CENSUS.md` to name "the four refused element classes" using that exact
citation. The maintainer's own audit comment caught this one anchor by
inspection, not exhaustively — it did not check `docs/virtual-hardware-parity.md`
or `docs/parity-contract.md`, both cited in §1 as landed prior art defining
"the bring-up method" and "the bound boundary"; **neither file exists in
this checkout** (`docs/` has no `parity*` or `virtual-hardware*` file at
all). Either this is a second uncaught branch-only citation, or these terms
are genuinely undefined on the real target branch, in which case "fidelity
binding," "bound boundary" and "observation point" — load-bearing vocabulary
used throughout §7 — have no normative source to point a reader at.
**Recommendation:** re-run the O1/O3/O4 verification against the actual
merge target (not the deleted branch) before pickup, per the issue's own
rule 6 — and re-derive O3's HDL-refusal claim and the two background-doc
citations from scratch rather than trusting the pinned commit.

### 4. [Med] DoD lets 75% of the drawn machine ship with no functional check
The Method requires all eight boundary files (`alu`, `regfile`, `decode`,
`loadstore`, `csr`, `clint`, `cpu`, `rv32-soc`) to be drawn, but DoD only
requires: *"boundaries beyond `alu` and `regfile` are drawn but not claimed
green."* Nothing beyond "loads through the ordinary loader with no special
path" (§7.3.1) gates the other six. A contributor can satisfy the letter of
this DoD by drawing six placeholder/incorrect boundaries — decode logic that
doesn't decode, a CSR file that's structurally present but wrong — so long
as `BRINGUP.md` never marks them green. Given the target is ~580 elements
across eight files and only two are independently verified, "the machine
exists as drawn boundaries" (the title's own claim) is compatible with six
of eight being silently wrong.
**Recommendation:** at minimum require the non-green boundaries to
round-trip (save/load byte-stable) and pass `AllElementsRoundTripTest`-style
sanity, or explicitly say in `CENSUS.md`/`BRINGUP.md` that unverified
boundaries carry no correctness claim whatsoever (the issue gestures at this
but never states it as a DoD line item).

### 5. [Med] Four "blocks execution" decisions are unresolved inside a fully-specified Method/DoD
§"Open Questions & Decisions Needed" lists four items explicitly tagged
**"Blocks execution"**: which CI lane runs the exhaustive ALU sweep,
per-tag vs. per-boundary census granularity, whether `machines/` lives at
repo root or under `test/fixtures/` (coordination with TASK-0016 and
TASK-0025 required), and whether `rv32-soc.jls` is the same artifact as
#73's sample CPU. Yet the Method section already commits to specifics that
presuppose answers (e.g., "per-tag" census in §7.10's equation, root-level
`machines/cpu-rv32/` throughout). If "blocks execution" is meant literally,
the Method/DoD sections describing the already-decided shape are premature;
if it isn't literal, the label is inflated. Either way a picker-upper cannot
tell which of the four is actually still open versus already baked in.
**Recommendation:** resolve the four questions (or explicitly mark the
Method's assumed answers as "recommended default, not yet ratified") before
this is treated as ready.

### 6. [Low-Med] H3's own falsification path is unbounded at file time
H3 anticipates refutation ("$2^{16}$ operand pairs per op is too slow even
for a long-run lane") but the mitigating lane assignment (`@Tag("longrun")`)
is itself one of the four unresolved "blocks execution" items above, and it
depends on TASK-0016 landing first ("once TASK-0016 lands"). So the
cost/feasibility risk this hypothesis exists to catch is deferred to a
prerequisite that is cited but not confirmed landed anywhere in this issue.
**Recommendation:** get a rough wall-clock estimate for one ALU op's
$2^{16}$-pair exhaustive sweep under `BatchSimulator` before committing to
"exhaustive at reduced width, sampled at 32 bits" as the shape for every op.

## What's solid
- O2's element census of the shipped fixture is accurate: `grep -c '^ELEMENT'
  test/fixtures/riscv-sum1to10.jls` returns 1038 in this checkout, matching
  the quoted breakdown exactly, and the file is 120,179 bytes as claimed.
- O4 (the `RegisterFile` exemption) is accurate: `test/jls/ElementSimulationGoldenTest.java:546`
  does carry the bare `"RegisterFile"` exemption string as quoted.
- `RiscvCpuGoldenTest.java`'s STEPS=34/HALF=1000 clock-vector description
  matches the source exactly.
- The census-as-*budget* framing (exact equality, not an upper bound — §7.10)
  is a well-reasoned design choice with a stated falsification path (H2) and
  correctly anticipates the "noise vs. signal" failure mode.
- The decision to keep `test/fixtures/riscv-sum1to10.jls` and
  `RiscvCpuGoldenTest` untouched, and to require every existing golden
  byte-identical, is a sound scope boundary that limits blast radius.

## Note on the checkout
This review's code-anchor checks were run against `/home/user/JLS` at
commit `5311625` ("Checkpoint: issue review snapshots"), which matches
neither the issue's pinned `evidence_commit` (2d0ca9d, confirmed deleted-branch-only)
nor the comment's cited surviving master (`8288226`). Findings 3-4's
non-reproduced anchors should be re-checked against whichever commit is
actually the merge target before this task is scheduled.
