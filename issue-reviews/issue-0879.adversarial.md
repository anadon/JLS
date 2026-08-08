# Issue #879: TASK-C232-2: the value plumbing carries the new type, the 61 defensive clones are deleted rather than ported, and the event loop is measurably no slower on the recorded k2000 baseline
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

This is one of the more rigorously-grounded issues in this tracker: its structural claims
were checked directly against the checked-out tree and hold up. `git grep -o "clone()"`
returns 59 in `src/jls/elem` and 2 in `src/jls/sim` (61 total, matching O1 exactly).
`Put.currentValue` is `protected @Nullable BitSet` (`src/jls/elem/Put.java:385`), confirming
the `null`-as-HiZ premise. `SimEvent`'s sealed `Payload` hierarchy has exactly three
`BitSet`-carrying records — `NewValue`, `MemoryWrite`, `TableOutput` — out of seven total
variants, matching "the three `SimEvent` payloads" precisely. The keystone-c-performance.md
figures (0.742 s / 2,331,793 events / 318 ns/event / 8,090 cycles/s / 37.6% value share) are
quoted verbatim and correctly from the actual document. A rough independent grep for
null-coerced BitSet sites in `src/jls/elem` returns 29 matches, corroborating O2's count.
The `Adder.java` T4 concern is real and specific: `carry.set(0, sum.get(bits))` at line 420
reads bit index `bits` from a value produced by `BitSetUtils.SumCarry`, which is exactly the
kind of off-by-one the issue warns about deciding *before* widening. All six named golden
test classes exist. Despite that diligence, several structural and process problems remain.

## Findings (severity order)

**1. [High] The performance "gate" (P6) has no stated enforcement, and its own source
document explicitly disclaims one.** `docs/capability-roadmap/keystone-c-performance.md`
§8.1 says: *"Add a `test/jls/` harness that measures it. It does not need to be a JUnit
assertion — a timing assertion in CI is a flake factory."* #879 nonetheless calls this "the
performance gate" throughout and lists it as a Completion Criteria checkbox: "[ ] The k2000
gate measured and reported ... no figure adjusted to fit." Nowhere does the issue say what
happens if the number comes in slower than 0.742 s. An executor who runs the harness once,
gets a 5% regression, and reports it honestly with fixture/JDK/flags stated has satisfied
the letter of P6 and the checklist item — the acceptance criterion cannot distinguish "gate
passed" from "gate failed but reported honestly." Recommendation: state explicitly that a
regression blocks merge (or names a waiver path with a successor issue, as done elsewhere in
this issue for #878), not just that it must be reported without rounding.

**2. [High] The #476/#362 scheduling contradiction is correctly identified but left
mechanically unenforced.** Verified directly: keystone-c-performance.md §7.2 item 4 states
*"Replace the global `dupCheck` with per-element pending-event slots ... **Sequence this
after the value type lands**, because an immutable value with a cached hash makes the
interim cost bearable"* and its §8.3 revised-sequence table puts the value type at stage 0
and the queue/dedup replacement at stage 0.5. But #362 (FEAT-030)'s own "Sequencing &
Parallelism" section states the opposite: *"TASK-0056 is independent of TASK-0063 and
TASK-0064 ... Two agents can run TASK-0056 and TASK-0063 concurrently."* #879 §4 names this
contradiction and says it "records" the belief on #362, #476 and #393 — but #879's own
`blocked_by`/`blocks` machine block contains no edge to or from #476, so nothing stops #476
(the queue/dedup task) from being picked up and landed concurrently with or before #879.
By the cited document's own reasoning, that would (a) make #476's dedup replacement pay an
avoidable interim cost and (b) violate T5 ("attribution"), which #879 lists as its own
threat. A comment recording a disagreement is not a scheduling constraint; either #879
should add a real edge (e.g. `879 blocks 476`) or #476 should be updated to block on #879.

**3. [High] The `#878` waiver escape hatch can be used to defeat the two-task split's whole
premise.** Completion criterion: "#878 has landed, or the dependency was waived with a named
successor." Confirmed live: #878 (TASK-C232-1, the type itself) is open, and `src/jls/core`
today contains no value type at all (`Bounds`, `Geometry`, `GridPoint`, `GridSize`,
`Orientation`, `SegmentGeometry`, `TextMetrics`, `package-info` only) — there is nothing to
widen the plumbing to. #878's own abstract states the split exists specifically "so the
byte-identity claim [in #879] is attributable." A waiver-and-proceed path formally exists in
#879's checklist, but exercising it would require #879 to invent its own ad hoc replacement
for `null`, defeating both the attributability rationale for splitting #878/#879 and P7 ("no
compatibility overload taking a bare `BitSet` value survives"). This escape hatch should
either be removed for this specific dependency or explicitly say the waiver is not available
here, unlike the generic boilerplate used elsewhere in the tracker.

**4. [Medium] P1's "all six golden suites... verified" enumeration repeats a list #442's own
audit already found incomplete.** Confirmed: `find test -iname "*GoldenTest.java"` returns
eight classes, not six — the same six #879 lists (`BatchSimulationGoldenTest`,
`ElementSimulationGoldenTest`, `SequentialGoldenTest`, `SimulationSemanticsRegressionTest`,
`VcdExportGoldenTest`, `RiscvCpuGoldenTest`) plus `VerilogExportGoldenTest`,
`VhdlExportGoldenTest`, and `PcfGoldenTest` under `test/jls/hdl/`. #879's own `blocked_by`
lineage cites #442, whose O3 finding is precisely that a "six"-item enumeration undercounts
the discoverable `*GoldenTest` set by exactly these three files. Neither `src/jls/hdl/*.java`
nor `test/jls/hdl/*.java` references `BitSet` (checked directly), so the three omitted
suites plausibly don't exercise the value channel and may be legitimately out of scope — but
#879 never says so; it just asserts the same "six, verified" framing #442 flagged as wrong.
Either state explicitly why the HDL export goldens are irrelevant to this migration, or add
them to P1's list.

**5. [Medium] Feasibility: this is a task-sized ticket with feature-sized blast radius.** In
one change: widen `Put`/`Input`/`Output`/`WireNet`, all three `BitSet`-carrying `SimEvent`
payloads, `TraceSample`, delete "61 minus N" clone sites across 23 files (each requiring
individual justification if kept), individually review and record 29 null-coercion decision
sites, extend two test classes with genuinely new cases (not just green re-runs), decide and
document `Adder`'s carry-bit shape before touching it, and hit a byte-*equality* (not
tolerance) bar across the whole engine — while explicitly forbidding any `react`-body
arithmetic change (H4) and forbidding landing alongside #476. T2 concedes the >64-bit
fallback has zero in-tree witnesses (confirmed: keystone-c §2/§9 record `maxBits=32`
tree-wide), so a real, plausible-to-break correctness path is exercised by nothing the
byte-identity gate can catch. This is honestly disclosed by the issue (T1/T2), but the
combination of "equality, not tolerance," "29 individually reviewed sites," and "untested
>64-bit path" is a large amount of surface for one task to close cleanly; a single missed
site among 29 is a latent defect the golden corpus is not guaranteed to surface.

## What's solid

- The dependency graph (`blocked_by: 878`, `blocks: 322/344/422`) is internally consistent
  with #878, #442, #476, #362 and #493 read directly — no fabricated issue numbers, no
  invented content.
- Evidence-commit hygiene is disciplined: it correctly uses `828822672fc3a8e2cb6da25192472079f04c29dd`
  (master) per #493's guidance rather than the dead branch commit `2d0ca9d` that many other
  issues in this sweep still cite.
- O1 (clone census), O2 (null-coercion site count), O3 (`TraceSample` HiZ marker), and O4
  (k2000 baseline figures) all reproduce against the live tree/doc, not just against a stale
  branch.
- T4's `Adder` carry-bit-width concern is a genuine, specific, verifiable landmine, and
  gating it behind a documented pre-decision (rather than discovering it mid-refactor) is
  good engineering discipline.
- H4's scope-split trigger ("no `react` body's arithmetic changes; if one did, file it
  separately") correctly anticipates the likely conflict with the `Adder` carry decision.

## Recommendation

Do not block on this review, but before execution: (a) make the k2000 performance figure a
real pass/fail gate with a stated regression policy, not just an honesty requirement; (b)
add a real ordering edge against #476 rather than a recorded belief; (c) remove or scope the
#878 waiver escape hatch so it cannot be used to proceed without the actual value type; (d)
either justify excluding the three HDL golden suites from P1 or add them.
