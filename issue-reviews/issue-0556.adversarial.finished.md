# Issue #556: FEAT-C29-1: every importer tells its losses in one voice — the .circ report becomes a format-agnostic contract of construct, disposition, location and explanation
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#556 asks for the loss-naming report format that CAP-16's FEAT-025 (#323)
builds for `.circ` to be generalized into a format-agnostic contract that
three not-yet-built importers (#558 `.dig`, #559 `.cv`, #561 Falstad text)
will consume. The boundary discipline against #323 is genuinely good and the
maintainer's own dedup comment already rules out merging with its siblings.
The acceptance criteria, however, describe a generalization that cannot be
honestly verified with the evidence available at the point this issue can be
worked, and the issue does not gate that fact.

## Findings, most severe first

### 1. [HIGH] The thing being generalized does not exist yet, and AC-3 is written as if it does

Grounding: `git ls-tree`/`Glob` confirm there is no `src/jls/imp/` package
anywhere in the tree, and #323 (FEAT-025) itself records `TASK-0054 ...
ABSENT at 2d0ca9d` and status `not filed` for the very reader/report this
issue proposes to generalize. AC-3 reads:

> "The existing `.circ` report is expressible in the shared contract with no
> information loss, demonstrated by round-tripping FEAT-025's report through
> the shared schema."

There is no "existing" `.circ` report to round-trip yet — #323 is unstarted.
The issue's `ordering_after` YAML field does sequence #556 after #323 in
prose, which is the right call, but nothing in the issue's machine block
(`blocked_by`) enforces that ordering the way #314 enforces its ordering on
#323 via `blocked_by: [315]` / `blocks: [323]`. A contributor who opens #556
today with only the prose ordering note to guide them could start "designing
the schema" against a report that doesn't exist, producing a paper contract
nobody has validated against a real artifact.

**Recommendation:** add a real `blocked_by` edge to #323 (mirrored per the
Link-pass convention already used elsewhere in this issue family), and
reword AC-3 to make explicit that it cannot be attempted, let alone closed,
before #323's TASK-0054 lands.

### 2. [HIGH] The contract is generalized from a single example, and the issue's own dependency graph makes that unfalsifiable before close

`ordering_after` names only `#323` and `#314` — not `#558`, `#559`, or
`#561`, the three consumer importers. Per #513 (CAP-29)'s own comparison
table, those three formats sit on three different substrates: XStream XML
(`.dig`), plain JSON (`.cv`), and "neither XML nor JSON" compact text
(Falstad). AC-4 requires:

> "The contract is a written document a new importer can adopt without
> reading another importer's source."

At the point #556 can close (after only #323 lands), there is exactly one
concrete implementation (`.circ`, XML) to generalize from. A "location"
field designed against one XML-based importer risks baking in XML-shaped
assumptions (line number + element path) that don't fit a JSON pointer or a
Falstad line/token address — and #556's own dependency graph means nobody
will find out until #558/#559/#561 are already underway, at which point the
fix is a REPLAN on someone else's issue rather than a reopening of this one.
The issue-556 dedup comment itself calls out #561 as "the falsification test
... against a non-XML, non-JSON source" — which concedes the schema is
unvalidated against non-XML input until #561 lands, yet #556's own AC set
lets it close before that validation happens.

**Recommendation:** either sequence #556's *close* (not just its start)
behind at least one non-XML importer actually adopting the contract, or
explicitly downgrade AC-4 to "documented, provisional, validated by #561"
and require a follow-up comment/REPLAN when #561 lands.

### 3. [MED] AC-1's "every importer" is unfalsifiable and gameable at the time this issue is workable

> "AC-1: One report schema ... is shared by every importer, machine-readable
> and human-readable, and golden-tested (CAP-29 AC-3)."

At the earliest point #556 can be worked, only the `.circ` importer exists.
"Shared by every importer" cannot be demonstrated with n=1: a golden test
against the sole existing importer can pass while the schema later proves
unusable for `.dig`/`.cv`/Falstad without anyone reopening #556 — the
verification illusion is that closing #556 looks like it produced a
four-format-ready contract when it has only produced a one-format-tested
one. This is the same root cause as Finding 2, surfacing here as an
acceptance-criterion wording problem rather than a sequencing problem.

**Recommendation:** rephrase AC-1 to say what is actually checkable at close
time ("the schema is shared by every importer that exists at close, and the
document states what a new importer must satisfy to conform") rather than
asserting universality over importers that don't exist yet.

### 4. [MED] No enforcement mechanism named for AC-2's "not re-implemented per format"

AC-2 requires the totality assertion (`C_src \ C_out = R`, inherited
correctly from FEAT-025 §3) be "provided by the shared infrastructure once,
not re-implemented per format" — but the issue names no concrete mechanism
(a shared abstract test base class every importer must extend, a single
totality-checker type importers call rather than reimplement, a lint/grep
CI check) to catch a future importer quietly reimplementing its own
totality test. Given #558/#559/#561 are separately estimated at 4-6, 3-5,
and 2-3 maintainer-weeks and will plausibly be worked by different
sessions/authors months apart, "not re-implemented" as a norm rather than a
compiler- or test-enforced constraint is exactly the kind of soft
requirement that erodes silently. Contrast with #314 in the same corpus,
which converts an analogous claim into a compile break (`setValue` return
type `void`→`boolean`).

**Recommendation:** AC-2 should name the enforcement artifact explicitly
(e.g., "each importer's report test extends `SharedReportTotalityTest`" or
similar), not just assert the outcome.

### 5. [MED] Cost band looks optimistic against what AC-1/AC-2/AC-4 actually ask for

Declared band is `1-2 mw` (matching CAP-29 PF-1's own `1–2 mw` line in
#513). In that budget the issue asks for: (a) extracting/generalizing a
totality-equality checker that doesn't exist yet (its `.circ`-specific
ancestor, TASK-0054, is itself separately estimated at 2 wk and unbuilt);
(b) designing a four-field schema whose "location" concept must eventually
span XML, JSON, and plain text; and (c) producing a document detailed enough
that a new importer author needs no other importer's source (AC-4). This
repo's own planning corpus flags exactly this failure mode elsewhere — #323
explicitly calls out "the band exceeds the row sum ... 3x at the high edge"
as an open question rather than silently absorbing it. #556 records no
equivalent caveat, despite arguably being at higher risk of under-costing
since two of its three deliverables (schema generality, adoptability without
cross-reading) can't be measured until formats that don't exist yet are
built against it.

**Recommendation:** carry an explicit "estimate assumes generalization from
one example; may need re-costing once #561 (the falsification test) lands"
caveat, mirroring the pattern #323 already uses for its own band gap.

### 6. [LOW] Unresolved merge conditional with no trigger or deadline

> "Per CAP-29's sibling rule: if CAP-16's REPLAN prefers absorbing this
> generalization, it merges there (lower number wins)."

This is a live conditional on an event (a REPLAN comment on #323) that has
no stated deadline or trigger criteria. Anyone picking up #556 must first
manually check #323 for such a comment before starting, and nothing in the
issue enforces that check. Low severity because the dedup-pass comment
already reduces the immediate risk of parallel duplicate work, but the
conditional itself remains open-ended.

**Recommendation:** either resolve the conditional now (the boundary
argument in #556 is strong enough to stand on its own) or give it an
explicit checkpoint (e.g., "recheck at #323's close-out").

## What's solid

- The boundary against #323 (owns `.circ` mapping semantics; #556 owns only
  report shape and totality infrastructure) is stated precisely and
  reinforced by the issue's own dedup-pass comment — low risk of the fork
  the comment worries about ("Merging any pair would fuse an XXE hardening
  review with a JSON-bounds review...").
- AC-2's totality condition, "equality in both directions," is correctly
  inherited from FEAT-025 §3's `C_src(f) \ C_out(f) = R(f)` and is precise
  enough to pin a test against once the infrastructure exists.
- Sequencing after both #323 (the concrete report to generalize) and #314
  (fail-loud loader, so a "drop" is an observable event rather than an
  inference) is the correct dependency pair in principle — the gap is only
  that it isn't machine-enforced (Finding 1).

## Note on repo state

No importer code of any kind exists in this checkout: `src/jls/imp/` is
absent, and the only structural precedent is the unrelated
`src/jls/hdl/imp/NetlistImporter.java` (Yosys JSON netlist import — a
different problem domain, structural HDL vs. schematic-capture geometry).
`docs/plan/features/` — the directory #323 cites as the source of its
ordering-edge derivation — does not exist in this checkout either, so that
provenance claim could not be independently verified locally.
