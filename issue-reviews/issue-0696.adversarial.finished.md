# Issue #696: TASK-C534-1: a triggering logic analyzer is a drawable element — edge, pattern and duration arming with pre-trigger history, serialized with the circuit
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#696 is TASK-C534-1, the first of three sub-tasks splitting #534
(FEAT-C23-5, PF-5 of capstone CAP-23 / #504) into an analyzer task (this
one), a word-generator task (#698), and a chronogram/VCD-integration task
(#700, which correctly declares `ordering_after: [TASK-C534-1, ...]` —
i.e. #700 already knows it needs this issue). The split itself is a real
improvement over #534: AC-3 here is scoped to the capture buffer alone
("a capture whose window includes the configured number of pre-trigger
samples") rather than #534's AC-2, which required "opens the chronogram
centered on the capture" — a dependency on unbuilt PF-1 that #698's
sibling review flagged. That narrowing is good hygiene and I credit it
below. But two problems carried over verbatim from #534/#698 are still
unresolved here: an acceptance criterion depends on a fixture that does
not exist and isn't scoped as work, and the cost-tolerance AC is circular
and ungated against the capstone's own funding precondition.

## Findings, most severe first

### 1. AC-3 depends on "the shipped hazard demo," which does not exist and is not scoped as work here

> "firing on the shipped hazard demo's runt pulse yields a capture whose
> window includes the configured number of pre-trigger samples"

I checked every `.jls` file in the tree: `test/fixtures/riscv-sum1to10.jls`,
`test/fixtures/fork-4.6-shiftregister.jls`,
`test/fixtures/headless-canary-gate.jls`, and `riscv/gui/cpu.jls`. None is
a hazard/glitch demo. `grep -ril hazard` over `src/`, `test/`, and `docs/`
turns up only prose discussion of the glitch hazard in
`docs/simulation-semantics.md:379-398` and `SimulationSemanticsRegressionTest.java`
— no circuit fixture. This is the same presupposition #534's own review
flagged (finding 1 there) against #504 §1 step 1 and #534 AC-2; it has
propagated into #696 unaddressed rather than being resolved when the task
was split out. As written, AC-3 cannot be executed until a hazard-demo
`.jls` fixture is authored and shipped, and #696 neither lists that as
in-scope work nor cites a dependency on whichever issue is meant to ship
it. **Recommendation:** either add "author and ship a hazard-demo fixture
circuit exhibiting a runt pulse" to this task's explicit scope (and its
`band_mw`), or add an `ordering_after` entry naming the issue that owns
it.

### 2. AC-5's tolerance is self-referential and the capstone's own funding gate for this exact task is uncited

> "kernel throughput with an unarmed instrument in the circuit matches
> baseline within CAP-23 AC-5's tolerance."

CAP-23 AC-5 is itself the source #696 points to for the number it needs —
there is no numeric tolerance defined anywhere in the repo (`grep -rn
"\bK9\b"` across all markdown: zero hits; no `ChronogramClosedCostTest` or
comparable throughput-ratchet test exists under `test/`, confirmed by
`grep -rln "PerfRatchet|BenchmarkTest|nanoTime|throughput" test/jls` →
only `SpatialIndexTest.java`, which isn't a timing test). Worse, CAP-23's
own Completion Criteria (per the #504/#698 review chain, independently
re-checked here for consistency) gate PF-3..PF-6 funding on "KC-23-1's
tap-cost measurement recorded from the demo slice before PF-3..PF-6 are
funded" — and #534 (PF-5, this task's direct parent) is inside that
range. #696 is a PF-5 sub-task and cites neither KC-23-1 nor any concrete
number; it inherits both the circularity and the funding-precondition gap
that #698's review already raised against its sibling task, unaddressed
here. This is doubly gameable: an implementer can invent any threshold
and call it "AC-5's tolerance" (nothing in the repo constrains it), and
even a conscientious implementer has no KC-23-1 baseline to test against
yet. **Recommendation:** cite KC-23-1 as a blocking precondition (or
argue explicitly why this task doesn't need it), and pin a concrete
number/method before work starts — e.g. "≤2% regression vs. a named
timing harness, median of N runs."

### 3. "Unarmed... in the circuit" (AC-5) is ambiguous between two very different baselines

Same ambiguity flagged against #534 finding 4: does the AC-5 baseline
circuit contain zero analyzer instances, or one placed-but-unarmed
instance? The former is a no-op comparison that would pass even if merely
*placing* an unarmed analyzer taxed every simulation tick (e.g. via a
per-tick virtual dispatch or an always-registered listener); the latter
is the actual zero-cost-when-inert claim the Outcome paragraph implies
("An unarmed analyzer is inert cost-wise"). As written a submitter can
satisfy the letter of AC-5 with the trivial reading. **Recommendation:**
state explicitly that the baseline circuit contains a placed-but-unarmed
analyzer element, not an analyzer-free circuit.

### 4. AC-2 and AC-4 name no test class or fixture format, unlike the discipline CAP-23 itself sets

> "All three trigger kinds... each asserted against a fixture whose
> firing tick is known."
> "a headless batch run can arm it, fire it and read the capture."

Neither AC names a concrete test class (contrast CAP-23's own ACs, each
pinned to a named test: `HazardDiagnosisWalkthroughTest`,
`InstrumentGoldenTest`, `ChronogramClosedCostTest`, etc., and contrast
#698's AC-3 in the same task family, which does name
`InstrumentGoldenTest`). "A fixture whose firing tick is known" and "read
the capture" are satisfiable by a single hand-picked, minimal fixture per
trigger kind that never stresses edge cases — e.g. a pattern trigger
tested only against a pattern that never partially matches, or a duration
trigger tested only with a duration well clear of its boundary tick. AC-2
also leaves "duration" undefined: is a minimum-duration trigger armed by
a level held for at least N ticks, or an edge held stable that long
afterward, and is "maximum duration" a trigger that fires when a level
is held *too long* (the opposite semantics from "minimum")? No grammar or
worked example is given, unlike `SigSim`'s concrete, tested `for`/`until`
stimulus grammar (`src/jls/elem/SigSim.java`) that a reviewer could point
to for comparison. **Recommendation:** name a test class per AC (as AC-3
of the sibling #698 does), and define "minimum/maximum duration" with a
worked example or explicit reference to the semantics doc.

### 5. `ordering_after: []` is plausible but unstated relative to #700's dependency on this task

#700 (TASK-C534-3) declares `ordering_after: [TASK-C534-1, TASK-C527-2]`
— i.e. #700 already knows it needs #696 to land first, specifically to
consume "the capture" that #696's AC-4 says a headless run can "read."
#696 itself never states what interface or data shape that capture
exposes for #700 to build the VCD-export/chronogram-centering logic on
(a `List<Sample>`? a raw bitset ring buffer? something serializable?).
This is not fatal — #696's `ordering_after: []` is defensible since #696
depends on nothing upstream — but the silence on the *downstream* contract
it hands to #700 leaves a coordination gap: #700 could be filed and
implemented against an assumed capture shape that #696's implementer
never agreed to. **Recommendation:** add one sentence naming the shape of
"the capture" (e.g. "exposed as an ordered list of timestamped samples
covering pre-trigger + post-trigger window") so #700 has a fixed contract
to build against.

## What's solid

- **AC-1** (ordinary element path, byte-identical save/load round trip)
  is concretely testable via the existing, proven pattern
  (`AllElementsRoundTripTest`, `CircuitRoundTripTest`) and needs no new
  infrastructure.
- **Scoping AC-3 to the capture buffer alone**, not "opens the
  chronogram" (contrast #534's now-superseded AC-2), correctly avoids a
  hard dependency on the not-yet-filed PF-1 chronogram work, and matches
  #700's `ordering_after` split — the chronogram-centering behavior is
  properly deferred to #700 rather than duplicated or forward-referenced
  here.
- **`band_mw: 1.5-2`** is internally consistent with #534's sub-task
  breakdown (#696 1.5-2, #698 1-1.5, #700 1) even though, as the sibling
  #698 review notes, the three bands together (3.5-4.5) slightly overshoot
  #534's stated 3-4 mw ceiling — a minor, already-flagged inconsistency
  rather than a new one introduced by #696.
- **No VCD/format scope creep**: unlike #534's own text, #696's body never
  claims a VCD-export or trace-format capability for itself, correctly
  leaving that to #700 — a clean task boundary.
