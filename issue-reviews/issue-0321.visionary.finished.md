# Issue #321: FEAT-019: a circuit drawn in JLS is readable by the open synthesis ecosystem — JLS writes the netlist Yosys reads
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Strip the machine block away and the claim is: *a circuit a student drew should
leave JLS into the open ecosystem — schematic renderers, browser simulators,
place-and-route, graders — and come back.* That is the right ambition, it is the
natural completion of the import work already shipped in `src/jls/hdl/imp/`, and
nothing below disputes it.

What I dispute is the mechanism the issue picks to get there, and the claim in
its Abstract that this is "one emitter, over the net partition and the HDL
intermediate representation that already exist." It is not one emitter. The
route it rejects for EDIF/BLIF/SPICE is the route already shipped for JSON, and
the route it selects quietly re-imports the lowering pass it just refused.

## 1. The pipeline this issue proposes to hand-write already exists, pinned and CI-exercised

`test/jls/hdl/imp/ImportPipelineTest.java:107-112` runs:

```
read_verilog <file>; hierarchy -auto-top; proc; opt_clean; memory -nomap;
wreduce -memx; opt; dffunmap; pmuxtree; techmap -map jls_map.v;
opt_clean; write_json <out>
```

with `test/resources/hdl/jls_map.v` (89 lines) as a JLS-authored techmap that
rewrites `$xnor`/`$eq`/`$ne` and friends into `CellValidator`'s accepted set.
JLS's Verilog emitter (`src/jls/hdl/VerilogEmitter.java`, 752 lines) is shipped,
and its goldens are compiled by real `iverilog` in CI
(`test/jls/hdl/IverilogCompileTest.java`). Compose those two facts and
"a drawn circuit becomes a Yosys `write_json` netlist" is **already true today**,
by a one-line script over artifacts this repository builds and tests.

The issue argues, correctly and at length, that EDIF/BLIF/SPICE should be reached
by running Yosys over JLS output rather than by writing emitters (§1 "Explicitly
out of scope", §2 rejection 2). It never applies that same test one level up.
Yosys is not only the back end that lowers to EDIF; it is also the *front end*
that reads Verilog. The `write_json` netlist sits behind the same subprocess, and
`ARCHITECTURE.md`'s recorded plugin-trust decision already says so: *"The
external tool integrations (#61 Yosys, #63 GHDL/Icarus, #62 ELK) already sit on
that subprocess boundary and stay there."* TASK-0045 moves one of them back
across it.

## 2. The refused lowering pass reappears inside the accepted scope

§1 declares direct gate-level emitters out of scope because they need a bit-level
lowering pass costed at 6-10 maintainer-weeks. Meanwhile the two comments on this
issue bind the writer to `V_w ⊆ V_r` — emit nothing `CellValidator` rejects.
`CellValidator.java:60-68` accepts nineteen cell types. Line up `HdlModel`'s
eleven statement kinds against them:

| Statement | Target cell | Status |
|---|---|---|
| `Gate`, `Constant`, `BitMap`, `Replicate` | `$and`/`$or`/`$xor`/`$not`, bit vectors | fine |
| `TriState` | `$tribuf` | fine |
| `Register` (`HdlModel.java:401`, three flavors, no async reset) | `$dff`/`$dlatch` | fine |
| `Select` | `$mux`/`$bmux` | fine |
| `Adder` (`:353`, carries `carryIn`/`carryOut`) | `$add` has no carry ports — needs width+1 extend and slice | a lowering |
| `Shift` (`:840`, variable amount) | `$shl`/`$shr`/`$sshr` **not accepted** | refuse, or synthesize a barrel mux network |
| `PriorityCase` (`:615`) | `$pmux` **not accepted** | must be lowered to a mux tree |
| `StateMachine` (`:725`) | no cell exists | must be lowered to mux tree + `$dff` |

`pmuxtree` is literally a pass in the pinned script above: Yosys already does that
lowering, for free, correctly. TASK-0045 would reimplement `proc` + `pmuxtree` +
part of `techmap` inside `src/jls/hdl/`, for JLS's two most pedagogically
important behavioral elements — the state machine and the truth table — and then
own that mini-synthesizer forever, at bus factor 1. Either that, or those
elements land on the refusal list, and "readable by the open synthesis ecosystem"
means "readable, unless you drew an FSM."

Neither branch is priced anywhere in Open Question 3.

## 3. I3, the close-out criterion, is a closed loop that today cannot close

I3 asserts `P(imp(emit(c))) ≅ P(c)`. Two problems.

**It is mostly unassertable.** `NetlistImporter`'s own javadoc (`:34-48`) says the
mapper realizes ports, `$not`/`$and`/`$or`/`$xor`, `$mux` and constants — and
that `$add`, `$dff`, `$dlatch`, `$tribuf`, the reductions, `$bmux`, hierarchy,
bit slices, concatenations and width mismatches are *reported as import problems*.
`HdlExporter.java:421-424` independently refuses `SubCircuit`, `Memory` and
`ShiftRegister` on the way out. The intersection of "exportable" and
"re-importable" is combinational gates and muxes. The fixture corpus is 70 files;
I3 can speak for a handful of them, and closing that gap is #61/#320's work, not
this feature's.

**It proves the wrong thing.** Emit-then-reimport compares JLS to JLS through a
cell vocabulary JLS defined on both sides. The issue rejects name-based
comparison as vacuous "whenever both sides derive names the same wrong way" — but
the same hazard operates one level up: if the writer and the mapper share a wrong
idea of what `$dff` means, the partitions match and the netlist is still wrong in
Yosys. Structural self-consistency is not ecosystem readability.

## Alternative A — ship the capability as a route, not an emitter (days, not weeks)

Make `jls -export circuit.json` real without a JSON emitter:

1. Promote the pinned script and `jls_map.v` from `test/resources/` to shipped,
   versioned artifacts (`resources/hdl/jls-export.ys`, `jls_map.v`).
2. On `-export foo.json`, JLS emits Verilog to a temp file, locates Yosys via the
   already-shipped `YosysLocator` (`src/jls/hdl/yosys/YosysLocator.java`), runs
   the script, and delivers the JSON. Yosys absent → one `jls: error:` line
   printing the exact two commands to run by hand, matching the import side's
   "explain, never disable silently" idiom.
3. TASK-0046's document then covers *four* routes over one mechanism (JSON, EDIF,
   BLIF, SPICE), not three routes over a second mechanism.

What this buys: real elaboration and technology mapping instead of a hand mapper;
FSMs, truth tables, shifts and carry adders work on day one; the cell vocabulary
cannot fork because there is only one producer of cells; zero new refusal list;
`CellValidator` remains the single gate. `docs/capability-roadmap/sweep-03`'s C9
section already reasons in exactly these terms — round-trip "pushed through the
fixed Yosys pipeline (`ImportPipelineTest.java:109-112`)" — so this reframing is
*recovering* a recorded direction, not inventing one.

The honest cost: export of JSON now needs a local Yosys. That matters for exactly
one consumer — pasting into a browser DigitalJS with no toolchain installed. Price
it before paying two maintainer-weeks for it: the subset a native writer could
serve unaided is the gate-and-mux subset, which is the least interesting circuit a
student draws, and every other consumer in the issue's own list (nextpnr, EDIF,
BLIF, SPICE, equivalence checking) requires Yosys anyway.

## Alternative B — replace I3 with the oracle that would actually catch a bug

The capability worth asserting is not "the partitions are isomorphic" but **"the
ecosystem agrees with JLS about what this circuit does."** JLS already exports VCD
(`VcdExportGoldenTest`, `docs/vcd-interop.md`) and has batch simulation goldens.
So: take a fixture, run JLS's own batch simulation, run the same vectors through
`iverilog` (or DigitalJS) on the exported design, and diff the waveforms. That is
a differential oracle against an outside implementation, it catches exactly the
mapper and semantics errors a partition check cannot see, it is skip-when-absent
like every other external check here, and it is the natural on-ramp to #369
(FEAT-053, equivalence and coverage) — which this issue currently treats as
*downstream* of the writer when in fact the semantic check is the whole point and
the netlist is only plumbing.

It also dissolves the comparator-duplication question the first comment raises
against #523/#627: with a semantic oracle here, the stable-id partition comparator
has exactly one owner (the KiCad line), not two.

## Alternative C — spend the freed weeks on provenance, which is the real gate

`sweep-03` C9 §2-§3 names what round-trip actually requires: `(* jls_id *)` /
`(* jls_type *)` attributes on emitted constructs, a `keep`/`keep_hierarchy`
preservation mode so `opt_clean` cannot dissolve them, and behavioral elements
emitted as black-box modules so an FSM comes back as an FSM instead of an
anonymous gate mesh. `YosysNetlist` already parses the `attributes` map. That work
is what makes the loop close for real circuits, and it lands in the *Verilog*
emitter, strengthening an artifact three other features already depend on.

## What I am explicitly disregarding, and what I would keep

**Disregarded:** integration criterion I3 as written (self-consistency loop,
unassertable over most of the corpus), I1's byte-exact JSON goldens over 70
fixtures (goldens for an artifact Yosys should be producing), and the TASK-0045
scope as a whole. Open Question 1 (schema pinning) evaporates — the pinned
artifact becomes the Yosys version, which CI already pins for import.

**Kept, and worth filing separately today:** criterion I5 is a genuine bug, not a
feature. `JLSStart.java:382-385` selects VHDL for any non-`.v` suffix that passes
the `:1088-1091` allowlist; that is a two-line fix plus a test and should not wait
on a synthesis roadmap. Also keep: the refuse-never-approximate principle (§3's
`μ` contract) — it is right regardless of mechanism; the `HdlEmitter` determinism
requirement; and TASK-0111, which is orthogonal to all of this and is where the
student-visible value actually is. If a canonical JSON write path in `JsonValue`
is still wanted, let TASK-0111's machine-readable grading report be the thing that
justifies it — that is a JSON document JLS genuinely owns.

**Net effect:** the capability statement survives intact; the 2.6-week writer-core
becomes roughly a week of plumbing and documentation, and the difference buys the
provenance work and the differential oracle — which is the version of this feature
that makes #304's round-trip claim and #369's equivalence work true rather than
merely scheduled.
