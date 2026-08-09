# Issue #415: TASK-0032: every record kind declares how it merges, in one table with a test per row, and the offline merger becomes the CRDT's executable specification
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This is an unusually well-evidenced issue — O1's measured `git merge-file`
failure (clean merge, `0xfff` on a declared-4-bit pin) is real, reproducible-looking,
and exactly the kind of finding this tracker should have more of. The design
(state-based STRICT/AUTO split, ops-not-text emission, offline-before-online)
is sound in outline. But the dependency bookkeeping has drifted out from under
two rounds of self-correction, and the most recent comment changes the
construction order without the body being amended to match — both are concrete
defects that will mislead whoever picks this up, not stylistic nitpicks.

## Findings, most severe first

### 1. [HIGH] `blocked_by: []` is factually wrong, and one of its two "unfiled" prerequisites was already filed *before this issue was*

The machine block says:

> `blocked_by: []  # TASK-0005 (reference elements by stable id, FEAT-003) and TASK-0031 (semantic validation of a merged file, FEAT-012) are both genuine prerequisites and neither is filed yet`

Checked against the tracker: **TASK-0031 is #409**, created `2026-08-03T15:18:10Z`
— eleven minutes *before* #415 itself (`2026-08-03T15:29:24Z`). The claim
"neither is filed yet" was false for TASK-0031 at the moment it was written.
**TASK-0005 is #436**, filed later the same day (`16:14:16Z`) and still open
as of today (2026-08-08). Both sibling issues independently name #415 in
their own Related Work / dependency graphs:

- #436's Related Work: *"#415 (TASK-0032) — per-record-kind merge rules, FEAT-012's other task. It consumes the reference form this task lands."*
- #409's `blocked_by` comment and its own mermaid both route into TASK-0032.

Comment 2 on this issue, posted the same day as this review (2026-08-08), did
a careful audit that added #279 to `related` and corrected two closed-issue
citations (#352→#170, #348→#169) — but did not catch that the two issues this
task's own body calls "genuine prerequisites" have been sitting filed and
open for five days with no `blocked_by` entry. §14's DoD item *"Every
`blocked_by` entry ... has landed — TASK-0005 and TASK-0031, whose issues the
link pass adds"* cannot be satisfied by a link pass that has now had two
opportunities (filing-time, and comment 2) and taken neither.

**Recommendation:** add `436` and `409` to `blocked_by` now (or file the
correcting comment this issue's own amendment protocol prescribes), and
verify neither #436 nor #409 shows execution progress before treating #415 as
unblocked.

### 2. [HIGH] Comment 2 inverts the STRICT/AUTO derivation direction; §8 and §7.4 were never updated to match

Comment 2 §4 states plainly: given #279 lands first of necessity, *"the
derivation direction stated in this body is inverted in practice... this task
derives STRICT from #279's shipped rules by removing the tiebreak"* and *"H2
is re-read as a claim to be tested, not a construction order."*

But the body's §8 Method checklist still reads, unedited:

> - [ ] Write `docs/merge-rules.md` with one row per `(record kind, situation)` ... from the taxonomy in `docs/capability-roadmap/lf-06-diff-merge-vcs.md`.
> - [ ] Implement `jls.merge.MergeRules` as the single representation ...

— i.e. build STRICT from the roadmap doc first, treat AUTO as a downstream
tiebreak. That is exactly the construction order the newest comment says is
now backwards. §7.10's formula (`R_auto` derived from `R_strict` by one
substitution) and §7.4's contract prose are likewise unedited. An executor
who reads the body and stops at the most recent comment addressing O6 (which
reads as fully superseding) could easily miss that comment 2's §4 quietly
changes the build order for the entire table — and would build "two,
incompatible answers to the same question," which is the specific failure
mode this issue's own Abstract exists to prevent (*"Building it offline first
is what makes it the executable specification... rather than a second,
incompatible answer."*).

**Recommendation:** this needs a body edit, not another comment — rewrite §8
and §7.4 to say the table is populated from #279's landed rules by removing
the tiebreak, with the roadmap doc as illustrative provenance only, not the
literal source of the STRICT column.

### 3. [MEDIUM] "one test method per table row" is inconsistent with the table's stated totality, and the gap is gameable

§7.4 requires `MergeRules` to be "total over the declared key space"
`(record kind, situation)`. §7.10 names $\mathcal{K}$ (kinds) × $\mathcal{S}$
(situations — the 10-row taxonomy at
`docs/capability-roadmap/lf-06-diff-merge-vcs.md:418-428`, confirmed present
in the repo). But §8 names only ten concrete test methods, one per
*situation* (`bothSidesAddDifferentElementsMergesBoth`,
`conflictingEditsToOneAttributeConflictUnderStrict`, etc.) — not one per
`(kind, situation)` pair. §8's own bullets separately name at least six kinds
(element, wire, geometry, memory image, checkpoint, per-view section). If
$\mathcal{K}$ has six members, a genuinely total table needs on the order of
sixty rows, not ten. P4's reflective "no row without a test" check
(`theTableHasNoRowWithoutATest()`) is satisfiable either way depending on how
finely `MergeRules`' key space is actually keyed — a coarse-grained
`MergeRules` (one bucket for all ~35 element types) would pass P4 with the
ten listed tests while falling well short of the title's promise that
*"every record kind declares how it merges."*

**Recommendation:** state $\mathcal{K}$'s cardinality and membership
explicitly in the body before implementation starts, and clarify whether
"one test method per row" means literally per `(kind, situation)` or per
situation with kind covered by parameterization inside the method.

### 4. [MEDIUM] P8/H3 — the task's own stated justification for building offline first — is not executable at pickup, and the DoD does not carve that out

Comment 2 §3, unrebutted: *"There is no 'state two replicas reach' to
compare against... Until #279 lands, P8 cannot be written, let alone run."*
Yet §14's DoD still lists P8 under "Every post-fix prediction in §5
verified" without qualification, and §10's H3 falsification criteria assume
`merge3(...)` vs. replica-exchange comparison is runnable. The Abstract's own
sell for doing this offline-first — *"what makes it the executable
specification and test oracle of the online CRDT"* — is precisely P8. If P8
is silently waived (permitted per rule 10 with a `WAIVED:` comment) rather
than blocking, the task can close having shipped the STRICT/AUTO table
without ever proving H2/H3, at which point the "oracle" claim in the title
becomes unverified marketing.

**Recommendation:** add #279 to `blocked_by` (overlaps finding 1) and treat
P8/H3 verification as gating close-out, not a rides-along nice-to-have; if
schedule pressure forces a split, say so explicitly rather than leaving the
DoD item ambiguous about whether it's blocking.

### 5. [LOW] Scope: a "task"-tier issue bundling a feature's worth of surface, gated on two other unstarted tasks and one comparable-scope sibling

The Method section spans: a new `jls.merge` package, a new `-merge` CLI flag
plus `.gitattributes` line, a git merge-driver binary contract across three
platforms, a seeded property-test harness driving `CausalBuffer` directly,
and a full fixture/importer/emitter corpus sweep (P9) before arming anything
— on top of depending on #436 and #409 (findings 1 and 2) landing first, and
coordinating with #279's shipped rules (finding 2). §11 itself flags the git
driver as "a maintenance surface at bus factor 1" that "should ship first and
independently," and Open Question 2 already floats splitting the driver out.
Given three hard external dependencies, none landed, this reads as
feature-sized work wearing a task label.

**Recommendation:** take Open Question 2's option (b) as the default rather
than a rides-along footnote — split the git-driver/CLI-flag slice into its
own task so the rule-table-and-merger core is reviewable and mergeable on its
own critical path.

## What holds up

- O1, O4, O5, O7, O9 are independently checkable against the current tree and
  are accurate: `CircuitOp`'s sealed `permits` list
  (`src/jls/collab/op/CircuitOp.java:34-37`) matches exactly; `CircuitOpReader`
  really does dispatch on a bare `String` (O5's trap is real);
  `test/jls/DeterministicSaveTest.java`, `CollabSecurityRatchetTest.java`, and
  `SocketConfinementRatchetTest.java` all exist as cited; `docs/merge-rules.md`
  and any `jls.merge` package are indeed absent from the tree (O8).
- Comment 2's own correction of O6 is itself verifiably accurate: spot-checked
  `ElementVocabulary.ALLOWED` at HEAD — `RegisterFile` and `FieldExtend` are
  both present (34 entries), `ElementRegistry.ALL` has 35, and `TestGen` is
  the one deliberate, test-pinned exclusion (`ElementVocabularyTest` asserts
  it). The comment's self-repair mechanism works when it fires.
- The STRICT-conflict / AUTO-tiebreak split and "never combine line by line"
  posture for geometry/memory/checkpoint sections (H4, P7) is a conservative,
  sound default, consistent with the ten-row taxonomy actually present at
  `docs/capability-roadmap/lf-06-diff-merge-vcs.md:418-428`.
- The exit-status contract (0/1/2) matches the project's existing, already
  documented CLI contract (README.md, `docs/batch-interface.md`) rather than
  inventing a new scheme.
- The evidence commit `2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` does not
  exist in this checkout (`git cat-file -e` fails) — but this is a
  tracker-wide, already-flagged problem (#493, open), not a defect specific
  to this issue; comment 2 already re-pins to `29afb26`.
