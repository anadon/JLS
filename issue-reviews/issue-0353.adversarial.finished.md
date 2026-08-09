# Issue #353: FEAT-005: a long batch run costs linear time and bounded memory — the stimulus parse, the load-time net walk and the waveform dump stop being superlinear
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The three code-level defects this issue targets are real and precisely
anchored: I re-verified all four evidence blocks against HEAD
(`3b6d6ec`) and every quoted line matches current source line-for-line
(`src/jls/elem/SigSim.java:64-91`, `src/jls/Circuit.java:1345-1369`,
`src/jls/sim/BatchSimulator.java:384-476`). That part of the issue is
solid — this is not manufactured work. The problems are in the
acceptance criteria and the scope boundary drawn around the dump fix.

## Findings, most severe first

### 1. The peak-heap claim for the VCD dump is not achievable within the issue's own declared scope

**Claim (Capability Statement, §1):** "Peak heap during a batch run
with `-vcd` does not grow with the number of dumped samples: the dump
is written as it is produced." Also Integration Criterion 4 speaks of
an "allocation-per-cycle band," and §3 says the writer's "peak live-set
term drops from Θ(|C|) to Θ(1)."

**Contradiction, from the issue's own evidence section:** Evidence
item 4 states outright: *"Every sample is retained in `eventTrace` and
`probeTrace` for the run's duration."* I confirmed this in
`src/jls/sim/BatchSimulator.java:24-44` (the two maps) and
`:140-180` (`afterEvent`, which unconditionally appends a
`TraceSample` per changed watched element/probe for the entire run
whenever `-vcd` or `-r` is active — the accumulation is gated only on
`vcdFileName != null || JLSInfo.printTrace`, line 144). TASK-0010's
stated contract is narrowly "`BatchSimulator.toVcd()` is replaced by a
sink-taking writer and deleted... byte-identically" — i.e. it touches
only the *rendering* of the already-fully-buffered trace, not the
accumulation into `eventTrace`/`probeTrace`. Making `toVcd()` stream
removes the three redundant copies (`StringBuilder` → `String` →
`byte[]`) documented in evidence item 4, but the dominant retained
structure — one `TraceSample` per value change, for every watched
element and probe, for the whole run — is untouched and still scales
with `O(samples)`, i.e. with run length. So "peak heap does not grow
with the number of dumped samples" is false for any run long enough
that `eventTrace`/`probeTrace` dominate, which is exactly the
50,000-cycle-and-up regime this feature is justified by.

**Why this matters:** Criterion 1's own 50,000-cycle heap budget could
pass this closing gate simply because the *dominant* allocator at that
scale (per evidence item 5, 95% of allocation) is `SigSim`'s string
concatenation, not the dump — so the composed benchmark may pass
without the heap-does-not-grow claim about the dump ever being true.
The stimulus-parse fix (TASK-0009) would carry the whole budget while
the dump claim quietly goes unfalsified. That is a gameable
acceptance test: it can pass while the stated capability ("dump
written as it is produced," heap-flat-in-samples) is not delivered.

**Recommendation:** Either (a) narrow the capability statement to what
TASK-0010 actually buys — "the dump no longer exists three times over
at write time" — and drop the "does not grow with the number of
dumped samples" wording, or (b) fold `eventTrace`/`probeTrace`
streaming into scope (a materially bigger change — it would need to
either write during `afterEvent` or accept out-of-order emission) and
say so explicitly, with its own cost line.

### 2. "Every waveform golden under `test/resources/`" points at a location that holds no VCD goldens

**Claim, repeated three times** (Capability Statement §1: "Every
waveform golden in `test/resources/` is byte-identical..."; §3 Tracks
durably: "The waveform goldens under `test/resources/`, unchanged in
content"; Integration Criterion 2: "Golden byte-identity across the
whole roster... pinned by the existing golden comparison").

**Evidence:** `find test -iname "*.vcd"` returns nothing, and
`test/resources/` contains only `test/resources/hdl/**` (Verilog/VHDL
fixtures for `HdlExporter`, unrelated to VCD). The actual VCD goldens
(`WAVE_GOLDEN`, `STIM_GOLDEN`, etc.) are `private static final String`
literals inline in `test/jls/VcdExportGoldenTest.java` (confirmed
lines 72 onward) and `test/jls/VcdProbeExportTest.java`. There is no
file under `test/resources/` for a byte-identity diff to run against.

**Why this matters:** This is the acceptance mechanism for the
riskiest child (TASK-0010, which changes *when* bytes are written) and
for the naming regeneration in TASK-0008. An engineer or a script that
takes the issue literally — "diff `test/resources/` before and after"
— will find nothing to diff and could wrongly conclude the golden-
identity gate is trivially satisfied. The actual check has to be
"re-run `VcdExportGoldenTest` and `VcdProbeExportTest` and diff the
`assertEquals` failures," which is a materially different (and
correct) procedure that the issue never states. As written, the
criterion's stated verification location does not match where the
oracle actually lives.

**Recommendation:** Correct the criterion to name the actual oracle:
`test/jls/VcdExportGoldenTest.java` and `test/jls/VcdProbeExportTest.java`
(and any other suite with inline VCD string goldens), not
`test/resources/`.

### 3. TASK-0008/TASK-0010 coupling is asserted, not evidenced by current code

**Claim (§3, Feature-Level Interface):** "the streaming writer emits
declarations whose names come from the naming function" and lists
`HdlExporter.java`'s "seven identifier families" as sharing a
convention with the VCD `$var` declaration writer inside
`BatchSimulator.toVcd()`.

**Evidence against a present-day coupling:** `grep -n "toVcd" src/jls/hdl/HdlExporter.java`
returns nothing — `HdlExporter` has zero code-level relationship to
`BatchSimulator` today. The VCD writer's names come from
`LogicElement.getFullName()` / probe strings (`BatchSimulator.java:398,401-407`)
and its short *identifier codes* from a purely local sequential
counter (`vcdId(next)`, `:413-418, 509-519`) — nothing derived from
`HdlExporter`'s "dense save index" or stable-id groups. So the
"shared naming task" (TASK-0008) is not consolidating an existing
shared dependency; it is *introducing* one between two previously
independent export paths (VCD identifiers, which the VCD spec treats
as freeform reference strings, versus Verilog identifiers, which must
satisfy Verilog's syntax). The issue does budget for a one-time golden
regeneration (§ Global Invariants #3) so it isn't silently assumed
byte-identical, which is good — but the critical-path ordering
("TASK-0008 → TASK-0010 → close-out... TASK-0008 before TASK-0010 is
necessity, not convention") rests on an architectural merger that
isn't independently justified in this issue; it's asserted as fact
inherited from TASK-0008/FEAT-004 (#336), which isn't fetched or
argued here.

**Recommendation:** Either link the concrete rationale for unifying
VCD `$var` names with Verilog export identifiers (constraints differ:
Verilog identifiers can't contain the characters JLS full names
commonly use, e.g. `.`/`/` hierarchy separators — worth checking
whether the "frozen convention" is even representable in both
domains), or decouple TASK-0010 from TASK-0008 and let the VCD writer
keep its current independent naming, revisiting only if FEAT-004
actually demands convergence.

### 4. "Doubling roughly doubles the time... within a tolerance that is a number in a test" is not a number anywhere in this issue

**Claim (§1):** both scaling assertions (`SigSim.initSim`,
`Circuit.finishLoad`) are gated on "a tolerance that is a number in a
test," and the wall-clock/heap budget for the 50,000-cycle run is
"declared" — but no number appears anywhere in the issue body. Every
concrete number the issue does cite (39.58/41.76 GB, 80,000 wire-ends
at 46s) is explicitly disclaimed in Evidence §5 as coming from a
different, later commit (`3a81a4a`) not in scope for this issue's
pinned evidence commit, and is never adopted as the target budget.

**Why this matters:** none of the four completion-criteria numbers
that would make this feature falsifiable (the wall-clock budget, the
heap budget, the doubling tolerance, the allocation-per-cycle band)
are fixed by the issue. They're deferred to "this issue's close-out"
(criterion 1) or "a maintainer decision" (Open Question 3, the
band-vs-sum gap). A feature whose numeric acceptance gate is written
by whoever closes it is a criterion that can be tuned post hoc to
whatever the implementation happens to achieve — the opposite of a
gate.

**Recommendation:** Pin at least the doubling tolerance and a rough
wall-clock/heap ceiling before work starts, even if provisional (e.g.
"budget derived from BRIEF.md, restated and re-derived against this
repo's current fixtures before close"), so an implementer can't retrofit
the number to match whatever ships.

### 5. Scope-boundary tension between "no format/VCD-profile change" and the naming-convention change

**Claim (Out of scope, §1):** "Any change to the VCD profile or the
`-t` grammar. `docs/batch-interface.md` owns both; this feature
changes allocation, not format." Yet TASK-0008's naming convention
changes the *content* of `$var` declaration names in the VCD output —
a change to what actually appears in the dumped file, adjudicated by
`docs/batch-interface.md` per the issue's own citation. This is a real
tension: the issue says format doesn't change, then carries a task
that changes VCD variable names (necessitating a one-time golden
regen), which is a content/format-adjacent change by any reasonable
reading, even if the VCD *grammar* (the `$var`/`#`/`$dumpvars`
structure) is untouched. This is not fatal — the issue does flag the
one-time regen — but the "changes allocation, not format" framing
undersells what TASK-0008 actually does to the emitted bytes and could
mislead a reviewer checking against `docs/batch-interface.md`'s
frozen-contract claims.

**Recommendation:** Either drop TASK-0008's naming change from this
feature's critical path (per Finding 3) so the "format" framing holds
cleanly, or amend the out-of-scope line to acknowledge the identifier
text changes even though the grammar doesn't.

## What's solid (no rework needed)

- The three core defects (quadratic string concat in `SigSim.initSim`,
  linear scan-per-`remove` on the `LinkedList` in `Circuit.finishLoad`,
  triple-materialized VCD string in `toVcd()`/`writeVcd()`) are real,
  correctly anchored, and independently fixable — verified against
  HEAD, not just the cited evidence commit.
- The `blocks`/`blocked_by` mirror with #354 is consistent: #354
  literally declares `blocked_by: [353]` with matching rationale.
- Invariant 1 (net-partition order must not change, citing
  `Circuit.java:76`'s insertion-order comment) correctly identifies the
  one real correctness trap in the `finishLoad` rewrite — a naive
  linear rewrite (e.g. iterating a `Set`/`Map` instead of a
  `LinkedList`) could silently reorder wire nets.
- The last-wins duplicate-signal-name behavior (Open Question 1) is
  correctly flagged as a semantic trap for anyone converting the
  `InputPin` scan into a `Map` lookup.

## Verdict rationale

Sound on the underlying defects, but two of the acceptance criteria
(Findings 1 and 2) would let the "integration criteria" pass without
the stated capability being true or without the stated verification
mechanism existing — that's a rework-before-filing issue, not a
nitpick. Findings 3-5 are scope/coupling risks worth resolving before
TASK-0008/0009/0010 are filed, since the critical-path ordering and
the out-of-scope boundary both lean on claims not fully supported by
current code.
