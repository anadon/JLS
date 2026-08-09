# Issue #607: TASK-C486-3: `jls -check` says whether a net is still a wire — l_crit = v*t_r/k per opted-in net, and "not assessable" everywhere else, including every shipped example
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This is the third of three tasks under FEAT-058 (#486), gated on FEAT-004
(#336), TASK-C486-1 (#603) and TASK-C486-2 (#605), all of which are
themselves open and unlanded. The physics and the worked numbers are
internally consistent (verified below), but the issue leans on an
entry point (`jls -check`) that does not exist in the codebase, points
at a fixture corpus (`examples/`) that does not contain what it claims,
and leaves the passing case of its own verdict untested by its own
acceptance criteria. These are not nitpicks — each one lets an
implementation that is wrong in a specific, predictable way pass every
stated acceptance criterion.

## Findings, most severe first

### 1. `jls -check` is not a real entry point, and the issue never says who adds it

`src/jls/JLSStart.java:759-789` (`FLAGS`) is the complete, authoritative
CLI flag table (`docs/batch-interface.md:25`: "The flag table in
`src/jls/JLSStart.java` (`FLAGS`) is the single [source of truth]").
There is no `-check` entry — the closest existing flags are `-b`
(batch mode), `-t` (test file), `-vcd`, `-export`. `docs/batch-interface.md`
never mentions "check" or a `-check` flag anywhere. `test/jls/CliFlagTableTest.java`
exists specifically to catch drift between this table and the docs.

Issue #607 uses `jls -check design.jls` as if it already exists ("The
lint... `jls -check design.jls` prints, per net..."). The parent feature
#486 flags this explicitly as **unresolved**: Open Question 4 — *"Is
`-check` the right entry point, or does the lint ride an existing
subcommand?... `docs/batch-interface.md` owns the grammar and **this
feature does not change it**."* That sentence is only true if `-check`
already exists to "ride" — it does not. So either (a) this task must
add a new CLI flag, a new exit-code path, and a `docs/batch-interface.md`
section — none of which appear in #607's five acceptance criteria or in
§3/§4's global invariants of #486 — or (b) the lint rides an existing
channel and every example in #607 and #486 that prints `jls -check
design.jls` is simply wrong. #607 resolves neither reading; it just
asserts the invocation as fact.

**Recommendation.** Before this task is implementable, settle Open
Question 4 on #486 and add the CLI surface change (flag table entry,
`docs/batch-interface.md` grammar section, exit-code contract,
`CliFlagTableTest` update) explicitly to #607's acceptance criteria, or
state which existing subcommand the lint output rides.

### 2. AC2's fixture corpus does not exist as described

AC2 requires: *"a corpus-wide test over the shipped `examples/` prints
that verdict on every circuit and no other verdict."* #486 repeats
this as its own integration criterion 2 and as CAP-18 AC-7.

At HEAD, `examples/` contains exactly one file: `examples/autograde.py`
— a Python script, not a single `.jls` circuit. `riscv/examples/`
likewise contains only `.s` assembly sources and `.clk.txt` files, no
`.jls`. The only `.jls` files anywhere in the tree are three fixtures
under `test/fixtures/` (`riscv-sum1to10.jls`, `fork-4.6-shiftregister.jls`,
`headless-canary-gate.jls`) plus `riscv/gui/cpu.jls`.

So "the shipped `examples/` corpus" that AC2 wants tested for "not
assessable" on every net either does not exist yet (in which case AC2 is
untestable as written and is quietly vacuous — an empty corpus trivially
satisfies "no other verdict"), or the intended corpus is actually
`test/fixtures/` and the issue is using the wrong name. This is worth
flagging pointedly because AC2 is also the falsification guard the whole
feature (#486 §7 "The lint fires on `examples/` (K18-3). The default is
wrong.") leans on — a guard over an empty or nonexistent set guards
nothing.

**Recommendation.** Name the actual corpus path and confirm it is
non-empty before treating AC2 as a meaningful test; if `examples/` is
meant to be populated with sample circuits as part of this or a sibling
task, say so explicitly, since it is not currently in scope anywhere in
the #486 tree.

### 3. The passing verdict ("lumped model valid") is never specified or tested

#486 defines three verdicts: `not assessable` (either input absent),
`lumped model valid` (ratio ≤ 1), `lumped model not valid` (ratio > 1).
#607 restates the vacuity case and one specific "not valid" example
(`net CLK`, 41x) but never states the wording, the boundary condition
(`≤` vs `<`), or a worked example for the *valid* case. AC1's worked
values (0.48 mm, 48.2 mm/70 mm, 434 mm/630 mm) are all bare critical-length
figures with no accompanying declared length, so none of them exercises
the ratio ≤ 1 branch at all; AC2 only ever exercises the vacuous default.
No acceptance criterion in #607 requires a test where a net declares both
attributes and turns out to still be a valid lumped model.

This is a concrete, gameable gap: an implementation that computes
`l_crit` correctly, prints "not assessable" correctly, and then has an
inverted or off-by-one ratio comparison (e.g. `ratio >= 1` instead of
`> 1`, or an entirely wrong string for the passing case) satisfies every
stated criterion in #607 while shipping a broken verdict for what will
be the *majority* of real declared nets (any net well under critical
length).

**Recommendation.** Add an explicit worked "valid" example and a
boundary-case test (ratio exactly 1) to AC1's table, and require
`ElectricalLengthLintTest` to assert the exact wording of all three
verdicts, not just "not assessable."

### 4. `k`'s default value and its user-facing surface are unspecified

AC1 requires "k a declared parameter, not a literal" and cites the
divisor family (D/5, D/6 Johnson, D/2) as the reason it must not be
hard-coded — but never states what the *default* value of k is, nor
where a user declares a different one (a CLI flag to `-check`, a
per-circuit attribute, a per-net attribute?). The worked values in AC1
are only reproducible with k=6 (verified below), a number that appears
only in #486's text, not in #607's. If #607 is meant to be a
self-contained task specification — its own YAML header format (`task_id`,
`part_of_feature`, `band_mw`, `ordering_after`) matches #603/#605, both of
which are readable standalone — this is a real omission, not just cross-
referencing convenience.

More importantly, "declared parameter, not a literal" is satisfiable at
the level of a Java method signature (an internal `computeCriticalLength(v,
t_r, k)` helper that a unit test calls with varying `k`) without ever
exposing `k` to a user of `jls -check` at all. That would satisfy AC1's
letter while defeating its stated purpose — "hard-coding one would ship a
tolerance choice dressed as physics." Nothing in #607 requires `k` to be
externally configurable end-to-end through the CLI/file format, only that
it not be a source-level literal.

**Recommendation.** State the default k explicitly in #607 (not only in
#486), and add an acceptance criterion that `k` is settable from outside
the JVM (CLI flag or file attribute) and that changing it changes
`-check`'s printed output on a fixture — otherwise "declared parameter"
reduces to "not a magic number in the arithmetic," a much weaker claim.

### 5. AC1's velocity column is ambiguous about scope: per-net attribute or default-only?

AC4 states "the default propagation velocity is settled and printed in
every verdict that used it," implying a single global default (FR-4,
per #486 Open Question 2's recommendation). But AC1's test table is
explicitly "(edge rate, declared length, **velocity**)," and its worked
values include both FR-4 (48.2 mm, 434 mm) and breadboard (70 mm, 630 mm)
figures at the *same* edge rate — i.e., two different velocities for the
same t_r. #486's own data contract is emphatic that only two attributes
exist per net (length, edge rate) and that FEAT-059/FEAT-060 "consume the
same two attributes and neither adds a third" — velocity/medium is not a
third declared attribute anywhere in #486, #603 or #605.

The likely intended reading is that `ElectricalLengthLintTest` unit-tests
the pure `l_crit` function directly with varying `v` (independent of
whatever `-check`'s end-to-end default is), while the CLI path always
uses the single settled default from AC4. #607 never says this, so a
reviewer or implementer has to reconstruct the distinction between "what
the unit test exercises" and "what the shipped tool actually computes"
from context spread across three other issues. Left as-is, it's exactly
the sort of ambiguity that lets a "breadboard" golden be satisfied by a
raw function call while `jls -check` itself never supports anything but
the one hard-coded FR-4 default — which may be intentional, but isn't
stated as intentional.

**Recommendation.** State explicitly that `ElectricalLengthLintTest`'s
`velocity` column exercises the pure formula and is independent of the
single end-to-end default `-check` uses, or else reconcile this with the
two-attributes invariant if velocity really is meant to be selectable.

### 6. AC1's third worked row drops its labels

AC1: *"18 ns → 434 / 630 mm"* — unlike the 2 ns row, which explicitly
labels "48.2 mm on FR-4 and 70 mm on breadboard," the 18 ns row gives two
bare numbers separated by a slash with no labels. The implementer has to
infer FR-4-then-breadboard ordering from the previous row's pattern.
For a golden test whose entire point is byte-exact reproducibility, an
implicit convention is a foot-gun — cheap to fix, so noted at low
severity relative to the others above.

**Recommendation.** Label both numbers explicitly, matching the 2 ns
row's style.

## What's solid

- The core formula (`l_crit = v·t_r/k`, `ratio = l_declared/l_crit`) is
  dimensionally correct and, at k=6, reproduces every worked value in
  AC1 to the stated precision (checked by hand: 20 ps → 0.482 mm ≈
  0.48 mm; 2 ns·FR-4 → 48.19 mm ≈ 48.2 mm; 2 ns·0.7c → 69.93 mm ≈ 70 mm;
  18 ns·FR-4 → 433.7 mm ≈ 434 mm; 18 ns·0.7c → 629.4 mm ≈ 630 mm).
- AC5 ("`WireNet.propagate` is untouched") correctly names the real
  method — `src/jls/elem/WireNet.java:443` is `public void
  propagate(@Nullable BitSet value, long now, Simulator sim)` — and the
  no-resimulation constraint is a clean, checkable boundary.
- The dependency ordering (`336`, `TASK-C486-1`/#603, `TASK-C486-2`/#605)
  is consistent with what those two issues actually declare as their own
  `ordering_after` (both list `367`), so #607 does not need to restate
  FEAT-047 directly — it inherits that gate transitively and correctly.
- "Not assessable, never PASS" as the vacuity guard for missing inputs is
  a genuinely good falsification-oriented design choice, clearly stated.

## Verdict rationale

Two of the six findings (the missing `-check` CLI surface and the
nonexistent `examples/` corpus) are blocking: as written, an
implementer cannot satisfy AC2 against reality, and cannot wire the lint
into the tool #607 repeatedly names as the deliverable's entry point,
without doing work this issue never scopes or budgets. The other four
are acceptance-criteria gaps that let a plausible, partially-wrong
implementation go green. None of this is unfixable — the physics and the
overall shape are sound — but the issue needs another editing pass
before a task-tier "band 1-2 mw" estimate can be trusted.
