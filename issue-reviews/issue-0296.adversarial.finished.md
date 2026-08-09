# Issue #296: CAP-00: close a decade of deferred maintenance behind eight standing ratchets that cannot silently regress
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what this issue actually is

This is not a normal bug/feature issue. It is a "capstone" tracking issue in a
machine-readable planning apparatus (YAML frontmatter, a mermaid DAG, nine
`requires_features` rows resolving to issues #314–#355, an external
prerequisite #337, kill criteria, a cost band, and four bot-generated
"REPLAN"/"ADJUDICATED"/"evidence-pin" comments). Underneath the apparatus are
eight genuinely small, verifiable code defects (D-01…D-08). I checked all
eight against the tree and they are real (see "What holds up" below). The
adversarial problems are almost entirely in the wrapper, not the defects.

## Findings, most severe first

**1. The evidentiary base is pinned to a branch that will never merge, and a
large slice of cited material does not exist on `master`.** The issue's own
frontmatter declares `evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`.
Comment 3 (`issuecomment-5171440145`) discloses that this commit "exists only
on a branch that will not be merged and will be deleted," gives a partial
line-renumbering table for two files, and states plainly: "citations into
`docs/plan/**`, `docs/machine-calibration.md`, `docs/parity-contract.md` or
`docs/virtual-hardware-parity.md` cannot be re-pinned at all — those 195 files
do not exist on `master`." I verified this against the checked-out tree: none
of `docs/machine-calibration.md`, `docs/parity-contract.md`, or
`docs/virtual-hardware-parity.md` exist. FEAT-009's entire justification (the
"tracked calibration fixture," the `riscv/build/k2000.jls` anchor, the
58ms/552ms kill-criterion numbers in KC-00-2) rests on a 1,124-line document
that is not in the repository anyone will actually branch from. An issue
whose acceptance machinery cites hundreds of files that don't exist on the
integration branch is not ready to be worked from as filed.
**Recommendation:** re-derive `evidence_commit` against a commit that is
actually an ancestor of `master` (or `master` itself), re-verify every quoted
line and file path against it, and drop or replace every citation into the
195 missing files before anyone starts FEAT-009.

**2. Massive scope aggregation disguised as a single closeable issue.** The
"Cost band" section prices the nine required features at 29–52
maintainer-weeks plus one explicitly unpriced row (FEAT-011's residual), for
a project whose own README/architecture docs repeatedly describe it as
single-maintainer ("bus factor 1"). That is 7–12+ months of one person's time
gated behind one issue, before counting the external, unfunded prerequisite
#337 (FEAT-015) that #316 needs and that this capstone "neither funds ... nor
lists" in its own words. The issue is aware this is a problem — it has a kill
criterion (KC-00-1) for exactly this reason — but a capstone whose own
authors need a numeric tripwire to stop it from ballooning is scope creep by
its own admission, not merely by outside criticism. **Recommendation:** split
into (a) the eight genuinely small D-01…D-08 fixes, which look like a
1–2 week PR series, and (b) the architecture-scale programs (SimpleEditor
decomposition, three-platform required CI, accessibility, installers) that
are separately justified initiatives already tracked elsewhere (#84, #91,
#162, #75, #76, #338) and don't need to gate on each other.

**3. Gameable/underspecified acceptance criteria.**
- **AC-3** ("0 of N jobs lack a timeout") checks only that the
  `timeout-minutes` key is *present*, not that its value is sane. A
  `timeout-minutes: 999999` on every job satisfies the letter of AC-3 while
  defeating its entire stated purpose (bounding CI cost / catching hangs).
  Nothing in §1 step 4 or AC-3 pins an upper bound on the value.
- **AC-7** ("at most K changed lines … K pinned by the fixture's own recorded
  value") does not state K anywhere in this issue — K is deferred entirely to
  a sibling issue's (#335/FEAT-009) not-yet-created fixture. A criterion whose
  numeric threshold is set by a different, not-yet-filed artifact is not
  verifiable from this issue alone at filing time.
- **AC-4** ("minimum value strictly greater than zero") is satisfiable by
  setting the `jls.edit` JaCoCo floor to e.g. `0.001` — technically nonzero,
  practically no regression protection at all. The issue's own worked XML
  example for AC-4 (§1 step 5 and AC-4's "literal shape required") renders
  with every XML tag stripped — the fenced block shows blank lines where
  `<rule>`, `<element>`, `<includes>`, `<limit>`, etc. should be — so the
  "predicate a cold reviewer can execute" is not actually legible as written
  in the issue.
**Recommendation:** tighten AC-3 to a maximum sane timeout value, not merely
presence of the key; state K explicitly (or explicitly mark it TBD-pending-#335
rather than presenting it as already a defined acceptance gate); fix or
re-fence the AC-4 XML example.

**4. A self-identified, unresolved integration hazard is shipped as "known
and unguarded."** §3 risk 1 states that FEAT-002 (#314, fail-loud loader) and
FEAT-003 (#334, format/reference change) land on the same code path with **no
ordering edge between them** in the filed feature tier (they are siblings
under #315, confirmed independently by the "ADJUDICATED" comment). The risk
text says outright: "this is the one ordering hazard in the set with a real
chance of a red default branch, and it is now unguarded." The mitigation is
punted to an unenforced social contract ("the two features must coordinate
directly"). An issue that files with a self-diagnosed "real chance of a red
default branch" and no enforced sequencing or allowlist contract is not done
planning. **Recommendation:** either add the missing `blocked_by` edge between
#314 and #334, or make the legacy-attribute allowlist a binding, tested
contract term before either lands — not a REPLAN-comment promise.

**5. Internal arithmetic/prose errors that survived multiple revisions.** The
issue's own text records that KC-00-1 was filed with wrong arithmetic ("1.5×
the top of the 35–62 mw band" computed as 63, when 1.5×62=93) and had to be
corrected in a later edit of the same issue. Separately, the delta paragraph
in §"Ordering rationale" claims "**Three** edges this issue previously drew do
not exist in the filed feature tier," then lists only two, and the
"ADJUDICATED" comment explicitly flags this as a known miscount left
uncorrected ("Left as written rather than edited, since no arrow is wrong").
For a document whose entire value proposition is machine-checkable precision
(a mermaid DAG, symmetric `blocked_by`/`blocks` audits, numeric kill
criteria), leaving a known factual error uncorrected "because it doesn't
change the graph" undercuts confidence in the rest of the unaudited prose.
**Recommendation:** fix the stray "Three" → "Two," and treat any future
arithmetic claim in this issue as requiring a second pass before merge.

**6. FEAT-008's price is wildly disproportionate to the defect it's nominally
closing, revealing the bundling problem concretely.** D-07 (the actual
defect) is "no PACKAGE rule for `jls.edit`" — verified against `pom.xml`:
lines 427-514 show PACKACKAGE rules only for `jls`, `jls.sim`, `jls.elem`,
`jls.collab.op`, and the `jls.edit` exemption comment sits at `pom.xml:408-409`
exactly as cited. Adding a `jls.edit` PACKAGE rule with a low nonzero floor is
a five-minute `pom.xml` edit. Yet FEAT-008 is priced at **12-20
maintainer-weeks**, by far the largest of the nine rows, because the issue
bundles the trivial floor-add together with decomposing the 5,852-line
`SimpleEditor.java` (confirmed: `wc -l` matches the cited figure) — work the
issue itself calls "residual" from #84/#91/#162, not required by D-07's literal
text. The acceptance criterion (AC-4) does not require the decomposition at
all; the feature that's supposed to satisfy it does. That's scope creep
baked into the required-feature mapping, not just in the discussion.

**7. Orphaned deliverable / diffusion of responsibility.** AC-1's central
artifact, `test/jls/DeferredMaintenanceRatchetTest.java`, does not exist yet
(confirmed: no such file in `test/`) and the issue states "no single feature
owns the suite; each feature contributes exactly one arm." Nine different
features/contributors are each expected to add one arm to a shared file none
of them is individually responsible for creating or keeping green as a whole.
This is a classic integration gap — normal in "N contributors, one shared
file" setups, but the issue doesn't assign an owner or a stub PR to seed the
file, so the eight-arms claim (AC-1) has no clear first mover.

## What holds up (spot-checked against the tree, not just the issue's own claims)

- D-01 (silently-dropped unknown attribute): confirmed at
  `src/jls/elem/Element.java` — all four `setValue` overloads loop over
  `savedAttributes()` and fall through with no `else`/throw, exactly as
  quoted.
- D-02 (dense, save-time-reassigned ids): confirmed at `src/jls/Circuit.java`
  around line ~1499 — `int id = 0; ... el.setID(id); id += 1;` over the
  stable-id-sorted list.
- D-03/D-04 (event-loop and batch-pause bugs): confirmed in
  `src/jls/sim/Simulator.java` (`dupCheck.remove(event)` before the
  `now > maxTime` check) and `src/jls/sim/BatchSimulator.java` (`pause`
  sets the same `stopping = true` as `stop`, parameter unread).
- D-05 (quadratic `SigSim` string concatenation): confirmed — four `+=`
  concatenations inside the per-line loop in `src/jls/elem/SigSim.java`.
- D-06 (0 of 23 CI jobs have a timeout): confirmed exactly — a real YAML
  parse of `.github/workflows/*.yml` gives 14+1+1+5+1+1 = 23 jobs, and
  `grep -rn timeout-minutes .github/workflows/` returns zero hits.
- D-07 (`jls.edit` unfloored): confirmed against `pom.xml`'s four PACKAGE
  rules and the exemption comment.
- D-08 (default container is XZ, not plain text): confirmed at
  `src/jls/FileAbstractor.java` — `writeCircuit(target, circuitText,
  Container.XZ)` is the two-arg overload's default.
These eight are real, narrowly scoped, and each individually testable — the
one part of this issue that is genuinely solid engineering triage. The
problem is everything built on top of them, not the eight defects themselves.

## Bottom line

The defect list is sound; the capstone wrapper around it is not ready. The
evidence commit doesn't survive to `master`, several cited documents don't
exist in the tree anyone would actually work from, three acceptance criteria
are gameable or numerically unspecified, one integration hazard is filed as
"known and unmitigated," and the cost/scope has already grown to the point
the issue needed to invent a kill criterion against itself. Recommend
splitting the eight small, verified defects into an immediately actionable
PR series, and re-filing the architecture-scale bundle (FEAT-008's
decomposition, three-platform required CI, accessibility, calibration
fixture) as separately-scoped work with its own, non-fictional evidence base.
