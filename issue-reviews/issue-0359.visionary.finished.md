# Issue #359: FEAT-023 (RESIDUAL): an emitter that produces valid-but-wrong output fails a build — the open toolchains become required behavioral witnesses, not optional compile checks
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the five roster rows away and one sentence remains: **JLS's HDL emitters have no
counterparty.** `VerilogEmitter` (752 lines) and `VhdlEmitter` (1149 lines) are checked
only against goldens JLS itself wrote, and `IverilogCompileTest` / `GhdlCompileTest`
check that those goldens *parse*, not that they *mean* what the drawn circuit means. A
sign flip in `assign {unc_2_1, net_2_0} = count_q + net_1 + 1'h0;` compiles perfectly and
reaches a student. That gap is real, it is the largest unguarded surface in the HDL
subtree, and closing it is squarely on the project's arc. ARCHITECTURE.md's own
"Equivalence criterion" for any future simulation strategy — *"it must agree bit-for-bit
with the #202 RV32I integration golden run as a differential oracle"* — is this same idea
already ratified for a neighbouring subsystem. Endorsed as a goal, without reservation.

The reframing is about *where the guarantee lives*. The issue puts it in CI policy: arm
the toolchains, make the required lane fail when a tool is absent, and let the presence of
`verilator` on an Ubuntu runner be what stands between a wrong emitter and a student.
That is the one architectural choice in this document that pulls against the rest of the
project, and it is load-bearing for four of the five §5 criteria.

## Reframing 1: invert the oracle — the golden fails the build, the tool proves the golden

JLS already solved this exact problem once, in this exact package, and the issue does not
cite the precedent. `test/jls/hdl/scan/YosysGroundTruthTest.java` says it plainly:

> "The corpus expectations are also hard-coded in `VerilogHeaderScannerTest`, so the
> scanner is still exercised without yosys — this test proves those expectations against
> an independent implementation."

That is a two-layer oracle. Layer 1 is a committed expectation that fails on every
machine, offline, with no toolchain. Layer 2 is the external tool, which does not gate
anything — it *audits the expectation itself*. Skipping layer 2 loses corroboration; it
never loses the correctness gate.

Apply that shape here and the feature simplifies dramatically:

- **Commit the behavior, not just the syntax.** For each HDL golden fixture, commit an
  expected trace: stimulus in the existing `-t` grammar, and the resulting watched-signal
  values. `mvn verify` runs JLS's own `BatchSimulator` over the fixture and compares
  against the committed trace. A semantically wrong emitter is caught by *whichever layer
  the emitted HDL feeds*, and the corpus is the same corpus.
- **The external simulator re-runs the same stimulus against the emitted `.v`/`.vhdl`,
  and is compared to the committed trace — not to JLS.** Same skip-when-absent idiom,
  unchanged.

What this buys, measured against the issue's own §5:

- Criterion 2 ("a knowingly wrong emitter is caught") becomes provable on a laptop with
  no HDL tools installed, on every platform, today. The issue's version of criterion 2 is
  only true on one armed Linux lane.
- Criterion 1 and criterion 3 (arming, skip-count reporting) stop being load-bearing.
  They remain worth doing as hygiene — TASK-0051 / #386 is a good task on its own merits —
  but a green build no longer *depends* on an apt line for its meaning.
- Open Question 2 ("which platform carries the required arming?") dissolves. So does the
  §7 replan branch about tolerances being widened until they pass: a committed expectation
  cannot be widened without a visible diff.
- The two-way JLS-vs-iverilog diff has a structural weakness the issue never names — when
  it goes red, *neither side is the reference*. A committed trace makes the divergence
  report answer "which one is wrong", not just "they disagree".

**I am explicitly disregarding the framing of §5 criteria 1 and 3 as the feature's
guarantee.** Arming CI is infrastructure hygiene; it should not be what makes a green
build mean the emitter is right. In a project whose every other contract is pinned by a
committed golden — `VcdExportGoldenTest`, `BatchSimulationGoldenTest`, `PcfGoldenTest`,
`FileFormatSpecTest`, `SaveTagsTest` — routing this one guarantee through a runner image
is the outlier, not the pattern.

## Reframing 2: the comparator is mostly already in the tree

The issue prices the unnamed differential leg at 5–8.5 maintainer-weeks (testbench emitter
2–3, waveform reader and comparator 2–4, fast-sim lane 1–1.5) and notes it has no TASK id.
That estimate treats three already-owned assets as new inventions:

1. **The testbench emitter is a transliteration.** `docs/batch-interface.md` §2.2–2.3
   specifies `-t` as piecewise-constant stimulus over absolute times: `a 0 for 10 0x1
   until 30 0 end` means a is 0 on [0,10), 1 on [10,30), 0 thereafter. That maps
   mechanically onto `initial begin a=0; #10 a=1; #20 a=0; end`. One normative,
   already-parsed grammar in, one `initial` block out — hundreds of lines, not weeks.
2. **The settling-point set does not need inventing.** §3's `S` is a new convention the
   issue leaves undefined. It is exactly the event times the `-t` file already declares,
   plus a settle epsilon. No new document, no new decision, and no risk of a convention
   drifting from the analog corpus's.
3. **The waveform reader exists twice.** `VcdExportGoldenTest` carries a spec-derived VCD
   parser, and `examples/autograde/autograde.py` carries a dependency-free one. Both sides
   of the comparison are VCD (`$dumpvars` on the iverilog side). The comparator is a
   VCD-vs-VCD diff over a declared signal set.
4. **The fixtures exist.** `HdlCircuitBuilder` builds each HDL golden's circuit in the
   on-disk text format and loads it through the real loader — *"the same technique as
   BatchSimulationGoldenTest"*. Every HDL golden is therefore already a simulable JLS
   circuit. The differential corpus is assembled; only the stimulus and the compare are
   missing.

The honest remaining cost is the semantic reconciliation, not the plumbing — and the
issue's own evidence shows why: `gate_delay.v` emits JLS's DelayGate as `assign net_1 =
a;`, and `counter.v` drops the Clock element entirely with a comment telling a human to
drive it. Timing and clock generation are *deleted* by the emitter, not merely different.
That is a genuine contract question, and it belongs in the emitter's own documentation.

Which surfaces a real defect in §3: it cites `docs/parity-contract.md` as the owner of
the permitted-to-differ framing, and **that file does not exist in the tree** (nor does
`docs/plan/`). The comparator's central semantic decision is delegated to a document
nobody has written. Writing it is arguably the whole feature; everything else is
mechanism.

## Reframing 3: for the combinational corpus, prove rather than sample

A simulation differential samples: it is only as good as the vectors chosen. For the
combinational goldens — `gate_*`, `mux`, `mux3`, `decoder`, `adder`, `extend`, `bundles`,
`constant`, `comb` — Yosys can do strictly better, exhaustively, and Yosys is *already
installed in CI today*:

```
read_verilog golden.v ; read_json jls_netlist.json ; miter -equiv -flatten ... ; sat -verify -prove trigger 0
```

A counterexample assignment is a more actionable failure report than "first divergence at
time t on signal q". There is no testbench, no stimulus corpus, no settling-point set, and
no vector-selection blind spot. The obstacle is that the miter needs a reference JLS emits
directly from its element graph — which is precisely **FEAT-019 (#321), the Yosys netlist
writer**, which this issue demotes to `related` on the grounds that it is merely
"beneficial". Under the equivalence framing #321 is not beneficial; it is the cheapest
route to the feature's own criterion 2, and it exercises a JSON dump of the element graph
rather than a second HDL syntax. `src/jls/hdl/yosys/` already reads that format
(`YosysNetlist`, `CellValidator`), so the writer is the mirror of shipped code.

Honest limits: sequential goldens (`counter`, register fixtures) need `equiv_induct` or
bounded checking, and JLS's two-states-plus-HiZ domain with multi-driver resolution
(`simulation-semantics.md` §2, §9) does not map cleanly onto Yosys's 4-state world — the
`tristate` fixture in `VerilogExportGoldenTest` is exactly where this breaks. So the right
answer is a **split corpus**: formal equivalence where it applies (most of the goldens),
trace differential where it does not. That split is a better cut than "one comparator for
everything", and it is invisible in the issue as written.

## Scope: the seam is cut along plumbing, not capability

Five roster rows are held together by "the same `ToolLocator`, the same
`Assumptions.assumeTrue`, the same nightly lane". That is an *implementation-mechanism*
seam. A recorded iCE40 hardware flash and an ngspice analog-tolerance corpus have nothing
in common as capabilities; bundling them because both call `findOnPath` is like bundling
every feature that opens a file chooser. The issue half-knows this — it concedes #264 owns
the board work (OQ3), concedes TASK-0100 is shared with FEAT-046, and concedes the
differential leg has no TASK id at all.

The dedup comment makes the consequence concrete: `#359 = #386 + #416`. #386 is CI arming,
#416 is the second board plus the hardware walk. **Neither is the behavioral comparator** —
the item §2 calls *"the single criterion that distinguishes this feature from what already
ships"*. The feature's filed children do not contain the feature's reason for existing. If
this feature is kept as scoped, that is the first thing to fix: file the comparator, and
let the board and analog rows go to #264 and FEAT-046 where they already have homes.

## What I would do instead

1. File **one task**: the committed behavioral corpus. `-t` stimulus + expected trace per
   HDL fixture, gated by JLS's own simulator, offline, on every platform. This is the
   feature. Its null test — break an emitter semantically, watch it go red on a machine
   with no HDL tools — is criterion 2, achieved without CI policy.
2. File **one task**: the external witness. Testbench transliteration from `-t`, iverilog/
   ghdl run, VCD-vs-committed-trace compare, skip-when-absent, non-gating. #386 arms the
   lane it runs on; the correctness claim does not depend on that arming.
3. **Promote #321** from `related` to a real prerequisite and evaluate the Yosys-miter
   route for the combinational subset before building any comparator. If it works, the
   waveform comparator shrinks to the sequential and tri-state remainder.
4. **Write `docs/parity-contract.md` first.** It is cited as normative and does not exist,
   and it owns the only genuinely hard question here — what the emitted HDL is supposed to
   mean when JLS's delays and Clock element have been erased from it.
5. Return the board and analog rows to #264 and #351. Keep #386 as filed; it is good work
   and needs no change beyond dropping the claim that it is what makes green mean anything.

The goal is right and overdue. The mechanism should live in the corpus, not in the runner.
