# Issue #291: HDL export: lift Memory off the reject list — RAM/ROM array with zero-delay async read, #199 sync-write process, in both languages
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is well-grounded on its four core code citations (verified against
`master`), but it is not currently workable as written: its own most recent
comment withdraws the machine block's `blocked_by` edge without the body
being updated, the recommended answer to its own Open Question 1 depends on
a mechanism (`REJECTED` map) that does not exist yet on `master`, and the
technical hypothesis (H1) omits the majority of `Memory`'s actual port
surface — CS/OE/WE and tri-state output — which is exactly what the issue's
named motivating consumer (#202's memory-mapped RV32I core) would exercise.

## Findings, most severe first

### 1. [High] The issue is internally self-contradictory: body vs. its own newest comment

The body's machine block declares `blocked_by: [61]` and `part_of_feature: 59`.
Comment `#5227291385` (2026-08-08, same day as the last update, by the issue's
own author/tooling) states explicitly: *"`blocked_by: [61]` is WITHDRAWN...
Re-derived: #61 is the **import** side... This issue is the **export**
side... this issue is unblocked by #61 today"* and separately *"Parent:
re-homed from closed #59 to **#873**"*. Yet the raw issue body — the text a
worker or an automated dependency-graph tool would parse first — still says
`blocked_by: [61]` and `part_of_feature: 59`. GitHub's `issue_read` API
confirms `parent.number = 873`, so the re-home *did* happen at the
tracker-relationship level, but the YAML block embedded in the body text
was never edited to match. Anyone (or any agent) trusting the machine block
literally will compute the wrong dependency graph. **Recommendation:** edit
the body's machine block to `blocked_by: [492]`, `part_of_feature: 873`
before this is scheduled, or require every consumer to read all comments
before touching the YAML.

### 2. [High] The issue's own recommended fix for Open Question 1 requires unlanded, unmerged infrastructure

Open Question 1 recommends: *"keep rejected with a teachable 'enable
synchronous write' message."* Comment `#5227291385` §4 explains why this
cannot be done yet: *"there is nowhere for that message to live"* because
`REJECTED` — the reasoned-refusal map from #492 — doesn't exist on `master`.
I verified this directly: `src/jls/hdl/HdlExporter.java:190-191` is a bare

```java
offenders.add(describe(el));
```

with no `REJECTED` map anywhere in the file (`grep -n REJECTED
src/jls/hdl/HdlExporter.java` returns nothing). `EXPORTED`/`SKIPPED`/
`TOPOLOGY` are the only three buckets (`:422-437`). #492 (open, unlanded,
with its own unresolved Open Questions about whether its four reason
strings are maintainer-endorsed) is a real prerequisite this issue's body
never names. **Recommendation:** either block this issue formally on #492
landing, or descope the "teachable message" half of Open Question 1's
default and ship the bare rejection form for the level-sensitive-write case
until #492 lands — but say so explicitly, since the current body's DoD
checklist still gates only on the withdrawn #61 edge.

### 3. [High] H1's technical model omits most of Memory's actual behavior — CS, OE, WE, and tri-state output

H1 states the plan as: *"an array declaration plus a continuous/concurrent
read (Verilog `reg [w-1:0] mem [0:cap-1]` with an `assign`/`always @*` read;
the VHDL array-type mirror) and a rising-edge write process."* Verified
against `src/jls/elem/Memory.java`:

- Memory's actual input ports are `address`, `OE`, `CS`, plus `WE` and
  `input` for RAM only, plus `clock` for sync-write RAM only
  (`Memory.java:184-196`).
- The output is genuinely **tri-stated**, not a plain continuous
  assignment: on `!(!cs && !oe)` it fires `TriStateOff` and
  `getOutput("output").propagate(null,...)` (`Memory.java:1392-1404`,
  `1448`, `1472`) — `null` is JLS's high-impedance value, the same
  mechanism the `TriState` element uses (`HdlExporter.java:549-558`,
  `HdlModel.TriStateStatement`).
- A write requires `type == RAM && !cs && !we && writeGate`
  (`Memory.java:1378`) — in sync mode `writeGate` is the clock-edge test,
  but CS and WE must *also* be asserted; a rising edge alone does not
  write.

None of Observations §2, Hypothesis §4, Method §8, or Interface §7
mentions CS, OE, WE, or tri-state at all. The literal reading of H1 — a
combinational `assign`/`always @*` read with no CS/OE gating — would
silently drop the tri-state and chip-select semantics that let multiple
memory-mapped devices share one data bus, which is precisely the
architecture a CPU's instruction/data memory (the issue's own named
consumer, #202's RV32I core) needs. **Recommendation:** add CS/OE/WE (and
tri-state `output`) explicitly to §4/§7/§8 before implementation starts;
reuse the existing `HdlModel.TriStateStatement` machinery the exporter
already has for the `TriState` element.

### 4. [Medium] Acceptance criteria are gameable: they never exercise CS/OE/WE or bus sharing

P2 ("ROM goldens compile... reads matching JLS batch output"), P3 ("a
sync-write RAM testbench (write, then read back)"), and §10's Falsification
Criteria (only "some access pattern" around zero-delay read-during-write)
describe single-instance fixtures with CS/OE presumably tied active the
whole time. An implementation that ignores CS/OE gating entirely (finding
3) could pass every literal P1-P4 check on such fixtures while remaining
unusable the moment two memories share an output bus with independent chip
selects — the shape #202's memory-mapped CPU would actually need.
**Recommendation:** add a fixture with CS toggled off mid-simulation
(asserting the exported net goes to `'bz`/`'Z'`) and a two-memory
shared-bus fixture to §8/§6 before this counts as done.

### 5. [Medium] A real ordering hazard is recorded only in prose, not in the machine-readable graph

Comment `#5227291385` §5 states: *"Sequence against #292, and the order is
not arbitrary. **#292 lands first**, because #292's instance-path
reject-propagation test needs a still-refused element two levels down as
its material and `Memory` is the designated fixture. Landing this first
removes #292's test subject."* This is a genuine "A must land before B",
but the body's `blocked_by` field says `[61]` (now withdrawn) and never
mentions #292 at all — `blocks: []` is also unchanged. An agent or
scheduler that reads only the YAML (the normal fast path for this kind of
fleet review) has no way to discover this constraint. **Recommendation:**
add `blocked_by`/`blocks` (or at minimum a `related`-with-warning) entry
tying #291 and #292 together in the machine block itself, not only in a
comment.

### 6. [Medium] "Small vs. large" memory threshold for Open Question 2 is undefined against real capacity limits

Open Question 2 recommends *"inline for small memories, file-based only if
contents are large"* with no numeric threshold. `Memory.java` shows the
real range this has to cover: `DENSE_CAPACITY_LIMIT = 1 << 22` (4M words,
`:1224`) and `MAX_INIT_WORDS = 1L << 24` (16M words, `:94`) — i.e., legal
JLS memories can be large enough that inlining their contents as Verilog/VHDL
literal initializers risks multi-hundred-MB to GB-scale generated HDL text,
which is exactly the kind of memory a synthesizable RV32I instruction store
(#202) would use. The issue marks this "rides along" with no threshold, no
file-based fallback design, and no test at the scale where it would matter.
**Recommendation:** pick and record a concrete word-count threshold (or byte
size) before Method step 6, and add a fixture near that threshold to the
golden set.

### 7. [Low] VHDL `--std=93` vs `--std=02` and the initialization-format decision are not cross-checked

P2 requires goldens to simulate under both `ghdl --std=93` and `--std=02`,
and separately Open Question 2 chooses inline-vs-file initialization. VHDL-93
has materially weaker array-aggregate syntax than later standards, and the
issue never states whether the same initialization strategy is expected to
work unmodified under both `--std` legs, or whether unequal fidelity between
Verilog and VHDL (or between the two VHDL standards) is acceptable. As
written, P1-P4 would treat each language/std leg as an independent
pass/fail without ever comparing them for equivalent behavior.
**Recommendation:** state explicitly whether `--std=93` gets the same
initialization emission as `--std=02`, or document the divergence per the
§10 pattern already used for TruthTable (PR #249 precedent, cited in the
issue itself).

## What's solid

- Observations 1-4 (current reject state at `HdlExporter.java:87-88` /
  `:422-428`, `memoryIsRejectedByName` pinning it, `syncWrite` defaulting
  off, and `time 0` being accepted by the loader) are all independently
  verified accurate against `master` — better-grounded than sibling issues
  #385/#384, whose citations this issue's own comment 3 found to reference a
  `REJECTED` constant that "does not exist on master and never did."
- The research-question framing (can Memory export as faithful HDL without
  changing JLS's own simulation semantics) is a reasonable, falsifiable
  question, and §11's threat about delta-cycle scheduling divergence between
  `iverilog`/`ghdl` around zero-delay read-during-write is a legitimate,
  correctly-flagged high-variance risk.
- Scope boundaries against #61 (import direction) and #358 (hierarchy) are
  drawn correctly and consistently across all three issues I cross-checked.
- The reject-path replacement discipline ("`memoryIsRejectedByName` ...
  intentionally replaced" rather than deleted) matches the project's stated
  policy-testing convention and is the right call.

## Note on scope of this review

I did not build or run the project. Findings 1-2 and 5 are corroborated
directly by the issue's own comment thread; findings 3, 4, 6, and 7 are
derived independently from reading `src/jls/elem/Memory.java` and
`src/jls/hdl/HdlExporter.java` against the issue text.
