# 13 — THE STRESS CORPUS: what exists, what it costs, and what breaks first

**Status:** survey deliverable. The maintainer asked: *"Are there public RISC-V
designs suitably large and high frequency enough to stress the system?"* This
document answers that, builds the bisection ladder CAP-17 said it needed, and
states the wall ordering measured against HEAD.

**Inputs:** five probe surveys, all read in full —
`corpus-cores.md` (fixed open RISC-V inventory),
`corpus-generators.md` (SoC generators as a scale knob),
`corpus-largest-open.md` (largest open hardware of any kind + the synthetic route),
`corpus-high-frequency.md` (the ~20 GHz channel corpus),
`corpus-jls-fit.md` (what JLS can ingest at HEAD and where it breaks).

**Repo state:** HEAD = `a2fc773` ("docs(plan): correct CAP-17's first move —
finishLoad, not the representation"). Re-verified in this session:
`Circuit.java:1345` still holds `LinkedList<WireEnd> ends`;
`FileAbstractor.java:65` still holds `MAX_CIRCUIT_TEXT_BYTES = 64L << 20`;
`NetlistImporter.importNetlist` still has **no caller in `src/`** (two test
callers only); `mapCell` still realizes `$not $and $or $xor $mux` and nothing
else.

**D10 is binding.** This is a survey of what exists and what breaks. Nothing
below argues that CAP-17 should or should not be funded, that any artifact
should be adopted, or that any defect should be fixed. §5's ordering is a fact
about HEAD; §6's experiment is the cheapest way to learn the next fact.

---

## 0. HOW TO READ EVERY NUMBER HERE

Four labels, never mixed:

- **MEASURED (published)** — someone else published it; URL given.
- **MEASURED (here)** — a probe ran the tool in this study; tool, version, flags
  and machine given.
- **DERIVED** — computed from a measured number; method shown.
- **ESTIMATED** — a judgement with a stated band. Used sparingly, always labelled.
- **not public** — looked for, not found. Never guessed.

**Three hygiene rules, all of which this study earned the hard way and all of
which are load-bearing below.**

1. **A cell count with no memory-handling flag is not a number.** VexRiscv linux
   is *either* 20,308 cells or 171,576 depending on one flag, and 88.2% of the
   larger figure is 72 Kib of SRAM that JLS models as single `Memory` elements.
   Every count below states its flag. Worst case found in this survey: Titan
   `LU230` holds 48,807,936 bits of SRAM in 5,056 primitives against 568,001
   total — an **86× swing** on the memory flag, worse than VexRiscv's 8.4×.
2. **A tile count with no hierarchy-handling flag is not a number either.** This
   is new, from probe 2. OPDB's 25-tile OpenPiton chip is 78,693,320 leaf cells
   flattened and ~3.15 M distinct cells hierarchically — the same file, a 25×
   spread. Preserving hierarchy converted it to Yosys JSON in **1 m 51 s**;
   flattening *one* of its 25 tiles ran 25+ CPU-minutes at 3.7 GB RSS without
   finishing.
3. **A methodological correction that invalidates labels elsewhere in this
   study.** Yosys 0.33's `synth` macro runs `memory_map` in its `fine` stage, so
   **bare `synth` already flattens memories**. Probe 2 proved it by running
   `synth` then an explicit `memory_map` and diffing: both `stat` outputs and
   both JSON files byte-identical at 221,212,228 B. To preserve memories you need
   `synth -run begin:fine` or `memory -nomap`. **Any cell count anywhere in this
   corpus quoted against bare `synth` is a memories-flattened number and should
   be relabelled.**

And one more, specific to this document:

4. **Do not convert LUT counts to JLS elements.** An FPGA LUT count scales with
   bit-width × logic depth; a JLS word-level element count scales with structure
   and is nearly width-independent. SERV vs PicoRV32 is **7.3× in Artix-7 LUTs
   and 1.5× in JLS elements**. The one LUT-based estimate in this document
   (XiangShan) is explicitly banded to an order of magnitude for exactly this
   reason.

---

## 1. THE DIRECT ANSWER

**On scale: yes for JLS's current wall, and decisively no for 10^10 gates.**
Rocket Chip ships pre-generated Verilog through LiteX's `pythondata-cpu-rocket`
(Apache-2.0) with a core-count knob that is linear to 0.02% and spans **31,404
to 462,047 JLS elements** for 1 to 8 cores — measured this study through Yosys
0.33, memories preserved — which straddles JLS's ~695,000-element single-file
ceiling on both sides and needs no Chisel build. Above that, the CAD-benchmark
world supplies pre-flattened RISC-V netlists no generator can match for
convenience: VTR's Titanium25 `rocket17` at **801,897** and `rocket31` at
**1,448,187** netlist primitives. And OPDB ships an OpenPiton ladder from 16
thousand to **~1.26 × 10^9** leaf cells with no toolchain but Yosys. But the
largest *fixed open RISC-V design that exists* is XiangShan Kunminghu 16-core,
whose only public size is **42.64 million LUT6 across 20 AMD VU19P FPGAs** —
of order **10^8–10^9 gates**, which is **12× to 23× short of 10^10**; and the
largest open artifact of any kind, OpenPiton 20×20, is still **~8× short**.
Nothing public closes that gap, so the 10^10 rung has to be generated (§3).
**On frequency: no, and not by a small margin.** The highest verified open
RISC-V ASIC clock is CVA6/Ariane at **1.7 GHz** in GF 22nm FDX; XiangShan Nanhu
is **2 GHz** in SMIC 14nm; the highest open FPGA fmax is picorv32 at **769 MHz**;
and the XiangShan 16-core FPGA prototype actually runs at **10.2 MHz**. Against
20 GHz those are one to three orders short, and no processor of any kind reaches
20 GHz — top stock commercial silicon is 6.2 GHz and the all-time liquid-helium
overclock record is 9.206 GHz. **20 GHz is not a clock; it is the Nyquist of a
40 GBd serial link**, so the frequency stressor is a *channel*, and no open
RISC-V design has a SerDes at all (§4). The two axes need two corpora and no
single artifact bisects both.

**The largest fixed open RISC-V design:** XiangShan Kunminghu 16-core CPU system
— <https://github.com/OpenXiangShan/XiangShan>, **Mulan PSL v2** (木兰宽松许可证，
第2版 — an OSI-approved Chinese permissive licence, *not* Apache-2.0; record it
exactly), last commit 2026-07-31. ASIC gate count and die area are **not public**:
the MICRO'22 paper PDF has been removed from `XiangShan-doc` (verified by
`git ls-tree -r HEAD` on a fresh clone), IEEE and readthedocs are proxy-denied,
and the project's own issue asking exactly this question —
<https://github.com/OpenXiangShan/XiangShan/issues/3638>, *"How Many LUTs Are
Required for the FPGA Minimal System…"* — is **open and unanswered**, its body
saying only that the official documentation gives no explanation.

**Scale gap:** 42.64 × 10^6 LUT6 at the conventional 10–20 GE/LUT6 band gives
4.3 × 10^8 to 8.5 × 10^8 GE (DERIVED). So **~10^9 gates against a 10^10 target:
one to one-and-a-half orders of magnitude, a factor of about 12× to 23×.**
Restated: the biggest thing the open RISC-V world has ever built gets you to
~10^9 gates and 10^10 is still an order of magnitude beyond it.

**The trap I nearly fell into, recorded so nobody else does.** The 10^10-gate
figure is a **platform capacity claim, not a design.** The same UniVista release
that gives XiangShan's 42.64 M LUTs advertises its cascade platform as scaling
to 160 VU19P and "百亿门级规模" — ten-billion-gate scale. That is the emulator's
ceiling. XiangShan 16-core uses 20 of those 160 slots. No open design reaches
the platform's ceiling.

---

## 2. THE CAPACITY LADDER

Ordered by **JLS runtime elements**, smallest first. The unit column is
load-bearing: a Yosys word cell, a Yosys leaf cell, a VTR netlist primitive, an
ORFS std-cell instance and a JLS element are five different quantities and this
table never silently converts between them. "JLS elements" is DERIVED via probe
1's validated cell→element mapping (calibrated against the study's own anchor:
VexRiscv_Min 724 word cells → **1,598** vs the study's independently derived
1,599 — one element apart) except where marked MEASURED.

**All Yosys counts below are memories PRESERVED** (`memory -nomap`, `$mem_v2`
survives as one cell → one JLS `Memory`) unless the row says otherwise.

| # | Artifact | Size, with unit and method | Licence | Obtain | Exercises | JLS wall |
|---|---|---|---|---|---|---|
| **0** | Study's hand-built RISC-V (`riscv.jls`) | **1,551 runtime elements** (MEASURED here); ~580 logic elements | study's own | in-tree | nothing — `finishLoad` 28.7 ms | none; every wall 100× away |
| **1** | **SERV** bit-serial | 524 word cells (Yosys 0.33, top `serv_rf_top`) → **~937 elements** | **ISC** | `git clone https://github.com/olofk/serv`, 18 MB | the low anchor; proves LUT orderings are wrong | none — fails on **correctness** |
| **2** | **PicoRV32** | 681 word cells (top `picorv32`) → **~1,400** | **ISC** | single 3,049-line `.v`, ~5 MB repo | **947 import problems on 681 cells** (MEASURED here) | none — correctness |
| **3** | **VexRiscv_Min** — the calibration anchor | 724 word cells → **1,598** | **MIT** | pre-generated `.v` in scratchpad; `pythondata-cpu-vexriscv` | the ruler; import correctness | none |
| **4** | **VexRiscv_Linux** — first Linux rung | 2,448 word cells → **5,009**. Contrast: 171,576 cells memories-FLATTENED, 88.2% being 72 Kib SRAM | **MIT** | as above | `$mem_v2` → `Memory`; the memory flag itself | none |
| **5** | **CVA6/Ariane** | total kGE **not public**; component figures only | **SHL-0.51** (not Apache) | `pythondata-cpu-cva6`, 30 MB — SystemVerilog, needs sv2v | 64-bit MMU core; **owns the frequency end** (1.7 GHz, GF 22nm FDX) | none |
| **6** | **`bc4000`** — the current import ceiling | **62,009 runtime elements** from 4,000 word cells; 35,008 `ELEMENT` records, 88.6% `WireEnd`; saveText 4,610,229 B (MEASURED here) | generated, public domain | `genv.py` + `yosys write_json` | **WALL A exactly**: 4,000 imports, 5,000 is `StackOverflowError` | **WALL A** |
| **7** | **Rocket `small_1_1`** | 15,767 word cells, 356 `$mem_v2`, 83,127 bits → **31,404** | **Apache-2.0 + BSD-3** | `pythondata-cpu-rocket`, 721 MB clone, one config's `.v` is 9.5–12.7 MB | first real large import; **exposes `$adff`** | none |
| **8** | **Rocket `linux_1_1`** | 41,733 word cells, 351 `$mem_v2`, 290,569 bits → **75,053** | same | same | **WALL B first bite** (10 s at ~81,000 elements) | **WALL B** |
| **9** | **Rocket `linux_2_1` / `linux_4_1`** | 73,138 / 136,994 word cells → **128,738 / 239,158** | same | same | WALL B in earnest; 60 s line at ~175,000 | **WALL B** |
| **10** | **Rocket `linux_8_1`** — last rung that fits in one file | 265,582 word cells → **462,047** (66% of the ~695,000 ceiling) | same | same | WALL B hard (hours at stock); WALL C at the dense end | **B, then C** |
| **11** | OPDB **`l15_wrap`** (L1.5 cache) | **357,432 leaf cells**, memories FLATTENED (Princeton's shipped BLIF, Yosys 0.9+4052) | **BSD-3** (this module is GPL-free) | `OPDB/baseline_BLIF.tgz`; `read_blif`+`write_json` = **10.4 s → 189 MB** | the rung immediately below JLS's ceiling | approaching **C** |
| **12** | OPDB **`sparc_core`** / Titanium **`rocket17`** | 1,003,242 leaf cells / **801,897 netlist primitives** (RAM blocks = 1 primitive) | **GPLv2** (OpenSPARC T1) / VTR per-circuit, **unverified** | 3.5 GB clone / ~1 GB tarball | **these do not open at all** | **WALL C** |
| **13** | ORFS **`nangate45/bp_quad`**, Titanium **`rocket31`**, Titan23 **`gaussianblur`** | **1,266,534 Yosys cells + 220 SRAM macros** hierarchical (MEASURED here; `-flatten` was **OOM-killed at 15 GiB**) / 1,448,187 primitives / 1,859,014 pre-packed blocks with **1,872,320 nets** | **BSD-3** / VTR / VTR | 392 MB sparse clone; 13 min, 2.4 GB / ~1 GB tarball | WALL C absolutely; `gaussianblur`'s net count is the artifact that proves or kills O(W²) | **C**, then **D** |
| **14** | Titanium **`mem_test_max`**, EPFL **`twentythree`** | **7,605,183 primitives** ("Can't fit on any S10 device") / **8,246,898 LUT-6** after ABC `if -K 6` | VTR per-circuit / **MIT** repo, Zenodo terms unverified | ~1 GB tarball / Zenodo | **WALL D on any machine**: 8.2 M × 1,190 B = **9.8 GB live** | **WALL D** — CAP-17's actual subject |
| **15** | OPDB **`chip` X_5__Y_5** (25 tiles) | **78,693,320 leaf cells** flattened; **~3.15 M distinct** hierarchically. BLIF→JSON **1 m 51 s → 1.47 GiB** (MEASURED here) | **GPLv2** | 3.5 GB clone, no Scala | the distributed rung. **94–169 GB live heap if flattened; 3.7–6.8 GB if the 25 tiles share one definition** | distribution |
| **16** | **XiangShan Kunminghu 16-core** | cell count **not public**; 42.64 M LUT6 on 20× VU19P → **10^7–10^8 elements ESTIMATED, band only** | **Mulan PSL v2** | Chisel; no pre-generated netlist exists | CAP-17 proper — the destination, not the corpus | distribution |
| **17** | OPDB **X_20__Y_20** (400 tiles) | **~1.26 × 10^9 leaf cells** (DERIVED: 3,145,960/tile × 400, cross-checked to 0.04% against the standalone tile BLIF) | **GPLv2** | pre-generated pickled Verilog | **the largest open artifact of any kind** | distribution |
| **18** | **10^10 gates** | **no artifact exists.** ~8× beyond rung 17, ~1,000× beyond the largest fixed artifact | — | must be generated | §3 | — |

**Three properties of this ladder worth stating separately.**

- **It is continuous.** There is no gap larger than ~3× anywhere between rung 0
  and rung 17, which is what "bisect against" requires and what one fixed
  artifact can never provide.
- **The convenient rungs and the permissive rungs are not the same rungs.**
  Rocket (Apache-2.0 + BSD-3) is permissive, RISC-V and scalable but tops out at
  462,047 elements. OPDB is scalable to 10^9 but is **GPLv2** and its cores are
  SPARC v9, not RISC-V. Titan/Titanium has the best RISC-V rungs above 10^6 and
  its per-circuit licences are **not public to this study** — they live in a
  ~1 GB tarball on a host that was proxy-denied. **No single artifact is
  permissive AND RISC-V AND scalable past 10^6.**
- **Above ~10^6 cells the corpus must be consumed pre-flattened.** MEASURED
  here: a 15 GiB machine flattens a 232,100-cell core (ariane133: 4.0 GB,
  18 min) and is OOM-killed flattening a 1.27 M-cell quad-core. The locally
  synthesisable flat-netlist ceiling sits between 10^5 and 10^6 cells — roughly
  three orders below the largest downloadable artifact. That is an argument for
  Titan BLIF / OPDB pickles / EPFL AIGER and against the RTL-generator world,
  independent of licensing.

**Two import blockers that only appear above rung 7, and that is the whole point
of having a ladder.** `$adff` (async reset — JLS `Register` has no async reset)
appears **123 / 159 / 199** times in Rocket `linux_1_1` / `4_1` / `8_1` and
**zero** times in VexRiscv, picorv32 and SERV. `$mul` (no JLS multiplier
primitive) appears 5 / 20 / 40 against 0–4 in the small cores. A Rocket import
fails 123+ times unless the RTL is rewritten to synchronous reset or the
importer gains a lowering rule. Note also that probe 1 costed `$adff` at 2
elements — the sync-reset rewrite — so **every Rocket element count in this
table is optimistic by construction: it assumes a fix that does not exist.**

---

## 3. THE SYNTHETIC STRESSOR SPEC

Nothing public reaches 10^10, so that rung must be generated. This is a
specification of what an honest generator would have to be, not a proposal to
build one (D10).

### 3.1 Four ways to be dishonest, each with a public worked example

- **(a) The chain of inverters.** Fanout 1, depth 10^10, zero registers, SCCs of
  size 1. It tests `malloc`. It is perfectly partitionable (any cut costs one
  wire) so it makes distribution look free, and perfectly compressible so it
  makes the file format look better than it is.
- **(b) The uniformly random mesh.** Rent exponent effectively 1.0: every
  partition of size *n* has ~*n* boundary terminals, so it makes distribution
  look impossible when real designs are demonstrably partitionable. It also
  destroys the fanout tail that real netlists have.
- **(c) Pure replication — and this one has a public name.** ITC'99 `b19` is
  literally two copies of `b18` cross-connected on datain/dataout buses; `b18` is
  two `b14` plus two `b17`; `b17` is three copies of `b15`. 231,266 gate lines
  that partition perfectly into four islands with a designed-in thin cut.
  **Replication is a valid MEMORY-capacity probe and an invalid
  DISTRIBUTED-EXECUTION probe, and those must not be run as the same
  experiment.** 10^10 replicated elements really do cost 10^10 elements of heap
  (~15 TB at 1,190–2,150 B/element); they tell you nothing about partitioning,
  diff/merge, event locality, or a format that dedupes subcircuits — which JLS
  already has.
- **(d) The EPFL MtM shape.** `twentythree` is 8,246,898 LUT-6 with **153 inputs,
  68 outputs, 36 levels**, "extracted from a set of random Boolean functions,
  generated with a custom computer program" (README, verbatim). I/O-to-gate ratio
  ~2.7 × 10^-5. A real 8 M-gate design has thousands of I/O and hundreds of
  thousands of registers. **MtM is the canonical example of a synthetic stressor
  that is honest about size and dishonest about structure.**

### 3.2 The nine properties, with measured targets

| # | Property | What it controls | Target, and where the target comes from |
|---|---|---|---|
| 1 | **Rent exponent p** (T = t·B^p) | whether distribution can ever be cheap; the inter-node message rate directly | **p ≈ 0.5** regular/SRAM-like → **≈ 0.75** random logic; 0.5–0.7 general subsystems (Christie & Stroobandt, IEEE TVLSI 9(6) 2001, DOI 10.1109/92.902258 — reached via secondary summary, flagged). **A generator that does not expose p is unusable for CAP-17.** |
| 2 | **Fanout / net-degree distribution, including the tail** | JLS wire-end count, hence `finishLoad` directly | mean net degree **2.5–4.0** (ANG's own recommendation). Independent anchor MEASURED here: **Yosys wire bits per cell = 2.49** (`bp_quad`), **1.31** (`ariane133`) — so ~1.3–2.5 wire bits per element, not the 1.0 a naive gate graph gives. **The tail must be added explicitly:** at minimum one net of fanout = register count per clock domain. For `bp_quad` that is a single net of fanout **260,428**, three orders above the mean and invisible to any average-based parameterisation. |
| 3 | **Logic depth and sequential depth, separately** | event-engine serialisation; also **WALL A**, whose recursion depth *is* the combinational depth | average topological depth **5–15** (ANG). Cross-check: EPFL `mem_ctrl` 25 LUT-6 levels, `arbiter` 18 are control-shaped; `hyp` 4,194, `sqrt` 1,033, `div` 867 are the arithmetic extreme and belong on a separate axis. |
| 4 | **Sequential fraction — a first-class knob** | how much state must be checkpointed, diffed and exchanged per cycle | **full SoCs 13.5%–22.5%**; datapath-only blocks 2.5%–9.0% (IWLS'05, Cadence RTL Compiler 180 nm). **Independently confirmed here twenty years later on a different ISA with a different tool:** ORFS `bp_quad` = 260,428 / 1,266,534 = **20.6%**, identical to three significant figures to IWLS'05 `leon3-avnet-3s1500` (185,025 / 899,632 = 20.6%). Use **20%** for a multicore SoC proxy, **8.8%** for a single core with hard caches (`ariane133`, MEASURED here). |
| 5 | **Feedback density / SCC structure** | whether the design levelises in one pass or forces iteration across partition boundaries | **not public.** No published characterisation of SCC size distribution in real netlists, and no generator exposes it. This is a genuine gap and it is the property most specific to a *simulator* as opposed to a placer. It would have to be measured from scratch — the obvious sources being the VexRiscv and BlackParrot netlists this study already holds locally. |
| 6 | **Memory fraction, as bits of SRAM per logic element** | everything; it is the flag that swings counts by 4× to 86× | **~3 bits of SRAM per logic cell**, from three designs spanning 62× in size, three tools: VexRiscv 73,728/20,308 = **3.63**; ORFS `ariane133` 544,768/187,936 = **2.90**; ORFS `bp_quad` 4,648,960/1,266,534 = **3.67**. At 10^10 cells that is ~3 × 10^10 bits ≈ **3.75 GB of modelled memory content** — a capacity problem in its own right, separate from the element table. |
| 7 | **Clock domains and crossings** | schedulability; the intersection with the HF axis | real designs run **1–11** clocks (ORFS, max 11 at `bp_quad`) with a long tail to **1,423** (Titan23 `sparcT1_chip2`, the only public design with that property at scale). Generate at both ends. |
| 8 | **Incompressibility** | whether the 64 MiB cap and the file format are honestly stressed | **the connectivity, not just the names, must have entropy proportional to size.** Randomised naming does not satisfy this — a compressor still finds a tiled topology. This is a JLS-specific requirement no CAD generator considers, and it is why §3.3's construction draws connectivity from a distribution rather than tiling a motif. |
| 9 | **Locality of activity, not just of structure** | the event engine's 3.14 M events/s | **not public.** Two designs with identical topology can differ 100× in events/s because one gates 90% of its registers most cycles. ANG, gnl and CIRC/GEN are all *structural* generators built for placers and routers, which care about neither #5 nor #9. **A CAD-derived synthetic gives an honest memory-capacity test, a possibly-honest partitioning test, and nothing reliable about events/s. There is no prior art to copy for either gap.** |

### 3.3 The construction, and the one hard constraint on its output

**Three levels, because a one-level generator always fails one of the nine.**

- **Level A — a library of leaf motifs at 10^3–10^5 elements, *taken from the
  real corpus, not generated*.** VexRiscv_Min (1,598 elements), VexRiscv_Linux
  (5,009), ORFS `nangate45/ibex` (17,244 std cells), ORFS `nangate45/jpeg`
  (68,062). A dozen motifs, each with a measured fanout distribution, depth
  profile, sequential fraction and memory-bit count. This defeats (a) and (d):
  the fine-grained structure is real by construction.
- **Level B — a Rent-obeying random hierarchy over those motifs**, gnl-style
  bottom-up clustering with a specified p, swept over {0.45, 0.55, 0.65, 0.75} to
  produce one corpus that should partition well and one that should not. This
  defeats (b) and (c): the topology above the motif is drawn from a distribution,
  so it neither tiles nor compresses, and its cut cost is *tunable* rather than
  accidental.
- **Level C — a global overlay**: clock trees, reset trees, and a small number of
  very high-fanout control nets that deliberately violate the hierarchy. Real
  designs have them; every generator in the literature omits them; they are what
  makes partitioning hard in practice and what makes the fanout tail real.

**The hard constraint: a generator that emits RTL is useless at 10^10 — it must
emit the flat netlist directly.** MEASURED here: Yosys flattened a 232 K-cell
core in 4.0 GB / 18 min and was OOM-killed (exit 137) flattening a 1.27 M-cell
quad-core with 15 GiB available.

**Instrumentation the generator must emit alongside the netlist**, or the corpus
is not usable as evidence: element count under **both** memory conventions;
wire-end count W; the achieved Rent exponent *measured back off the generated
netlist* rather than assumed from the parameter; fanout, depth and SCC-size
histograms; compressed and uncompressed file size against the 64 MiB cap; and a
golden simulation trace of N cycles so a distributed run can be checked for
*equivalence* rather than for not crashing.

**The size ladder to generate:** 10^5, 10^6, 10^7, 10^8, 10^9, 10^10. The first
three are checkable against real designs at the same size — §2 supplies one at
each — which is how you find out the generator is honest before trusting it at
10^9 where nothing real exists. **That is the whole value of the real corpus:
the real designs are not the stress test, they are the calibration standard for
the stress test.**

### 3.4 The one runnable prior artifact, and what it lacks

**ANG / artnetgen** — <https://github.com/daeyeon22/artificial_netlist_generator>,
**GNU GPL v3** (verified: `LICENSE` begins "GNU GENERAL PUBLIC LICENSE Version
3"). Implemented as OpenROAD commands; emits a real gate-level netlist against a
real LEF; `-num_insts` is unbounded. Its six parameters, verbatim from the
README: `-num_insts`, `-num_primary_ios` (~10% of insts), `-avg_net_degree`
(2.5–4.0), `-avg_net_bbox` (0.1–2.0), `-avg_topo_order` (5.0–15.0),
`-comb_ratio` (0.75–0.95).

**What is absent:** no Rent exponent (implied by `avg_net_bbox`, a *placement*
proxy useless to JLS, which has no placement); no memory fraction; no
clock-domain count; no feedback/SCC control; no switching activity. And
`-num_primary_ios` at 10% of instances means **10^9 top-level ports at 10^10
instances**, which is absurd and shows the ratio was calibrated on 10^5–10^6-
instance blocks, not chips. Its successor paper (ArtNet, arXiv 2510.13582, not
retrievable in this study) is explicitly motivated by ANG's two limitations —
"cannot handle macros" and "high runtime complexity … impacting usability in
modern contexts where designs can be macro-heavy and have huge scale" — which
are exactly the two that would bite a 10^10-element JLS stressor.

**gnl** (Stroobandt, Ghent) is the generator that gives property #1 directly and
by construction, but its distribution terms are **not public to this study** —
`users.elis.ugent.be` returned 403 — so do not assume them.

---

## 4. THE HIGH-FREQUENCY ANSWER

### 4.1 Why it is a channel and not a core

| Bound | Value | Source |
|---|---|---|
| Highest stock commercial CPU clock | **6.20 GHz** (i9-14900KS, Thermal Velocity Boost) | Intel product spec |
| Highest sustained server-class | 5.5 GHz (IBM Telum II / z17) | Hot Chips 2024 |
| All-time record, liquid **helium** | **9.206 GHz**, one core, one run | HWBOT |
| Highest open RISC-V **ASIC** | **2 GHz**, SMIC 14nm (XiangShan Nanhu) | project README |
| Peer-reviewed open RISC-V ASIC | **1.7 GHz**, GF 22nm FDX (CVA6/Ariane) | Zaruba & Benini, IEEE TVLSI 2019 |
| Highest open RISC-V **FPGA** fmax | **769 MHz** (picorv32, xcku3p -3, author's own Vivado P&R, binary search on period) | picorv32 `scripts/vivado/table.txt` |
| XiangShan 16-core FPGA prototype | **10.2 MHz** on 20× VU19P | UniVista/BOSC, 2025-04-09 |

20 GHz is **3.2×** the fastest stock clock and **2.2×** the liquid-helium record.
The one genuine exception — RSFQ superconducting logic, with a T-flip-flop
demonstrated to 770 GHz at 4.2 K — has no public design corpus, no open PDK and
no downloadable netlist, and the same literature puts the maximum *system* clock
around 120 GHz. It is a boundary marker, not a stress artifact.

**What 20 GHz actually is:** the Nyquist of a **40 GBd** symbol stream, sitting
between PCIe 6.0 (32 GBd, 16 GHz Nyquist) and 802.3ck (53.125 GBd, 26.5625 GHz).
Any 802.3ck- or PCIe-6-class channel model — characterised well past 40 GHz —
contains the 20 GHz point as ordinary interior data.

**And there is no open RISC-V design with a SerDes.** The only open-source SerDes
found is `SparcLab/OpenSERDES` (Purdue, **GPL-3**, Sky130, **last commit
2022-03-26**, data rate **not public**), and it is attached to no CPU. Every open
PCIe/Ethernet effort instantiates vendor hard IP for the PHY
(`chili-chips-ba/openPCIE` uses the Xilinx Series-7 PCIe hard macro and GTP
transceivers); AIB (CHIPS Alliance, **Apache-2.0**) is real open high-speed I/O
but is *parallel* die-to-die with no CDR, no equalisation and no channel model;
Basilisk, the end-to-end open Linux-capable 130 nm SoC, tops out at USB 1.1.
**The honest claim is "none found, and the ecosystem's structure explains why —
analog high-speed I/O is the one block nobody open-sources", not "none exists".**

### 4.2 Why a reference channel beats any design: it is a golden test

A design gives you a *self-consistency* test — JLS agrees with JLS. A reference
channel plus **COM** (Channel Operating Margin) gives you **input file → one
scalar in dB → tolerance**: JLS agrees with the industry. COM collapses a channel
plus a reference TX/RX into a single number, specified by IEEE Std 802.3
Annex 93A (25G class) and Annex 178A (200G/lane class), adopted by 802.3ck-2022
and by OIF CEI. **That shape — external input, external expected output, stated
tolerance — is the only thing in this entire survey that can falsify JLS rather
than merely load it.**

### 4.3 The artifacts, and the licence line that decides everything

| Artifact | Licence | Rate / band | Terms, stated plainly |
|---|---|---|---|
| **SiSoft IBIS-AMI Eval Toolkit v2.21** — `tx_impulse_no_eq.csv` 26,366 B / `tx_impulse_eq.csv` 26,382 B, 1,044 lines, header verbatim `* 5 Gb/s, 8 samples/symbol, 128 symbols`; plus 4-tap TX FFE **with C source**, `.ami` file, and a **reference execution environment** for Linux and Windows | **permissive**, read verbatim from `license.txt`: *"Permission to use, copy, modify, and distribute … for educational, research and commercial purposes, without fee and without a signed licensing agreement, is hereby granted"* + *"All distributions must include copies of all the original files in this kit."* | **5 Gb/s**, 200 ps UI, 25 ps sample interval, 25.6 ns record | **Redistributable. Ships with its own answer.** The whole-kit clause deserves a look before *deriving code* from it; as an unmodified fixture it is unambiguous. `git clone https://github.com/IBIS-Library/IBIS-AMI-test-kits`, 12 MB |
| **scikit-rf 2.0.1** — 19 bundled `.sNp`, incl. `wr1p5,line.s2p` 17,016 B, **500–750 GHz**, 201 points | **BSD-3-Clause** | data to **750 GHz** — 37× the 20 GHz point | **The smallest redistributable artifact whose data passes 20 GHz.** But they are 2-port waveguide lines with S11 = S22 = 0.0 exactly: no reflections, no mode conversion, no crosstalk, **no eye at all**. Golden answer available: "the parser round-trips". `pip install scikit-rf`, 585,098 B sdist |
| **PyChOpMarg 3.1.2** — COM in pure Python, **3,970 lines** across 21 modules | **BSD-3-Clause** | 802.3-22 Annex 93A / 178A | The reference-answer *harness*, already written under a GPL-compatible licence. `src/pychopmarg/matlab.py` defines `run_com_matlab(chnl_sets, cfg_sheet, matlab_exec)` — **it is designed to be differenced against the MATLAB reference.** Ships **no channels**. Self-classified `Development Status :: 3 - Alpha`. `pip install PyChOpMarg`, 45,358 B |
| **IEEE `802-COM`** reference code | **BSD-3-Clause**, IEEE SA Open, approved by OSCom 2024-12-19, public April 2025 | Annex 93A / 178A | The standard's own COM code, now absorbable. **The licence is free; the interpreter is not** — it is MATLAB `.m` with Excel config I/O, and whether it runs under Octave is **not public**. Host was proxy-denied; **flagged for human verification.** |
| **IEEE 802.3 task-force channels** — 802.3ap ATCA (`peters_01_0605_B12_thru.s4p` family), 802.3ck, 802.3df/dj | **NOT redistributable.** IEEE's terms: contributions *"remain the property of the respective copyright owners"*; use outside the standards project *"requires permission from the copyright owners"*; downloads permit *"one (1) copy … for your personal use"* | to **≥50 GHz** | **The real thing, free to fetch, not free to ship.** ~2.4 MB per `.s4p` (DERIVED from Touchstone line arithmetic); 0.24–1 GB per task-force set. **A CI job that downloads one and a repository that contains one are legally different acts, and IEEE's "one copy for personal use" wording does not obviously bless the CI job either.** |
| **Cadence IBIS AMI Eval Toolkit v3.1** — 10 Gb/s, 32 samples/UI, full input **and expected-output** vectors | **PROPRIETARY.** Header verbatim: *"In no event shall Recipient distribute the Licensed Material or use the Licensed Material for benchmarking purposes."* | 10 Gb/s | Listed **as a warning**. It is technically the better golden test and JLS may not use it. Its presence in a public GitHub repo is not a licence grant. |
| **PCIe compliance channels** | **not public** | to 32 GBd | Defined against *physical fixtures* (CLB, CBB, variable-ISI boards) that *you* characterise with a VNA. Requires PCI-SIG membership or your own instrument. |
| **`vectfit3.m`** (Gustavsen/SINTEF) | **NOT open** — *"Embedding the program code in any commercial software is strictly prohibited"* | — | **GPL-incompatible, must not be absorbed.** Widely and wrongly described as public domain. The absorbable replacement is scikit-rf's `vectorFitting.py`, 2,665 lines, BSD-3, with passivity enforcement documented. |

### 4.4 The consequence, and it inverts CAP-17's assumption

**The HF corpus is tiny in elements and unbounded in steps.** DERIVED: a 4-port
S-parameter channel fitted with pole-residue vector fitting is 16 matrix entries
× 20–100 poles, one state node per pole, lowered to `R C L V G F E` — **10^2 to
10^3 JLS elements**, i.e. 0.02%–0.15% of the 695,000-element file ceiling. The
largest open HF hardware artifact found, OpenSERDES, is **3,893 logic cells**
across serializer + deserializer + CDR (DERIVED by probe 4 from the shipped
`*.lvs.v` LVS netlists, physical-only fill/decap/diode/tap cells subtracted; the
SPICE netlists agree exactly at 4,790 and 9,542 instances) ≈ 4,000–6,000 JLS
elements.

Meanwhile at 20 GHz the period is 50 ps, so a 1 fs lattice is **50,000 steps per
cycle**; at the study's measured 3.14 M events/s that is **16 ms of wall time per
simulated cycle, 5.3 minutes per simulated microsecond**, and **~505 years** for
the 5.0 × 10^16 steps of a bit-by-bit BER 1e-12 run at 20 Gb/s.

**So: the gate axis bisects on element count → memory, `finishLoad`,
partitioning. The frequency axis bisects on time-base resolution × simulated
span → numerical precision and step-count throughput. A single artifact
bisecting both would be a 10^10-gate design containing a 20 GHz SerDes — a
datacentre switch ASIC or a leading-edge GPU. No such thing is public in any
form under any licence. CAP-17 should not expect the HF work to hand it a large
artifact.** A 26 KB CSV can be a harder simulation than a 60 MB netlist.

---

## 5. WHICH WALL COMES FIRST

**MEASURED against HEAD `a2fc773`.** OpenJDK 25.0.3, 4 vCPU, 15.7 GiB RAM,
default max heap 4,215,275,520 B, against `target/jls-5.0.5-SNAPSHOT.jar`
verified newer than every file in `src/`. The repository was not modified; the
patched class in §5.2 was compiled to a scratch directory and shadowed the jar
on the classpath.

### 5.0 The wall that precedes all the others: there is no import path

`NetlistImporter.importNetlist` has **no caller anywhere in `src/`** —
re-verified this session, two test callers only. The CLI flag table
(`JLSStart.java:759-788`) holds `-h -b -i -s -t -d -p -v -r -vcd -export -board
-pins -savetext` and **no `-import`**. The GUI's Import menu imports a subcircuit
from the already-open file, not a netlist. **So the import ceiling is not "5 of
19 cell types" — it is zero designs, reachable only from a test harness.**

And the binding constraint inside that path is not the cell table either. The
whole-vector connection rule (`resolveReader`, `NetlistImporter.java:718-745`)
resolves an input only when its exact bit vector is driven as a whole by exactly
one element. MEASURED here: a module of `y = {a[3:0], a[7:4]}` — **zero Yosys
cells** — fails to import. Realizing all 19 validated cell types would not
change that. On PicoRV32: **947 problems on 681 cells** — 524 bit-slice/concat,
349 unrealized cell, 74 validator violations, and every one of those 74 is
delivered to the user as *"This is a JLS import bug, not an error in your
Verilog — please report it"*.

### 5.1 The ordering, with the element count at which each bites

| Order | Wall | Bites at | Failure mode | Scope |
|---|---|---:|---|---|
| **A** | `HeuristicLayeredLayouter.longestPath` stack recursion | **combinational depth 4,000–5,000**; **62,009 runtime elements** in the probe shape | `StackOverflowError`, uncaught, no partial circuit | **import path only** |
| **B** | `Circuit.finishLoad`'s O(W) `LinkedList.remove` membership test | **≈81,000** elements at a 10 s budget; **≈175,000** at 60 s; 319,997 elements measured at **488,958.6 ms** | soak, not a crash | **every load** |
| **C** | 64 MiB decompressed container cap | **418,268** elements (dense, wire-free — bisected: 418,000 opens at 67,065,503 B, 418,270 refused at 67,109,151 B) to **~903,000** (terse importer output); **694,700** at the study's hand-built shape | hard refusal at open | every load |
| **D** | heap | ~500,000 on a 4 GB laptop at the study's pessimistic 2,150 B/el; ~5.1 M on 16 GB at the measured 822 B/el | `OutOfMemoryError` | every load |
| **E** | default simulation time limit (10^8) | **never, on size** | n/a — it is the `-d` flag | simulation |

**The answer: WALL A on the import path, WALL B on everything else. The 64 MiB
cap the brief lists first is THIRD. Heap is FOURTH on any machine with 8 GB or
more.** The study's ~695,000-element ceiling is reproduced exactly — DERIVED:
67,108,864 B ÷ 96.6 B/element (measured on `riscv.jls`) = **694,700 elements**.

Note the units carefully. Three different "element counts" circulate and differ
by up to 1.9×: **`ELEMENT` records** (what the cap weighs), **runtime elements**
(what the heap holds — includes `Wire` objects with no `ELEMENT` record), and
**wire ends** (the *only* quantity `finishLoad` cost depends on). Measured
wire-end fraction: 52.2% on the hand-built RISC-V, 50.0% on synthetic chains.
**The wire-end threshold is the robust number; the element conversion is not** —
an importer emitting Splitter/Binder meshes would raise the fraction and lower
every element-denominated threshold proportionally.

### 5.2 The measurement that reorders CAP-17's funding

A **two-line diff** — `LinkedList<WireEnd>` → `LinkedHashSet<WireEnd>`, and
`ends.remove()` → `ends.iterator().next(); ends.remove(end)`:

| circuit | runtime elements | wire ends | stock `finishLoad` | patched | speed-up | `stateHash` |
|---|---:|---:|---:|---:|---:|---|
| `ch20000.jls` | 79,997 | 39,998 | 8,894.5 ms | **221.8 ms** | **40.1×** | `54932712` = `54932712` |
| `ch40000.jls` | 159,997 | 79,998 | 37,227.4 ms | **466.5 ms** | **79.8×** | `33a3510a` = `33a3510a` |
| `ch80000.jls` | 319,997 | 159,998 | **488,958.6 ms** | **650.1 ms** | **752×** | `f59cd1b2` = `f59cd1b2` |
| `ch200000.jls` | **799,997** | 399,998 | not attempted (≥3.5 h at the quadratic floor) | **4,776.2 ms** | — | `7610f168` |

**Bit-identical circuit state in every comparable case.** `LinkedHashSet`
preserves insertion order, so the wire-net partition visits ends in exactly the
file order it did before — the determinism guarantee (#98) is preserved by
construction and the identical hashes are the evidence.

The W = 79,998 → 37.2 s row **independently reproduces the study's given "80,000
wire ends at 46 s"** to within cold-JVM spread. And the last row is the planning
number: **799,997 runtime elements — past the study's 695,000 ceiling — assemble
in 4.8 seconds**, on a stock 16 GB box. With the patch, the 64 MiB cap is
unambiguously the next wall.

**The curve is worse than quadratic at the top.** The last octave (W 80k → 160k)
costs **13.1×**, not 4×: local exponent **3.71**. The probe attributes this to
the `LinkedList` spine falling out of last-level cache; that causal claim is an
inference. The planning consequence does not depend on the cause: **any
quadratic extrapolation upward is a floor on the pain, not an estimate.** At the
64 MiB cap the honest range is 42 minutes (quadratic floor) to ~2.8 hours
(carrying the measured exponent, which is not sound across 2.3 octaves). Either
way: **a circuit at the cap does not load in a sitting.**

### 5.3 What this tells CAP-17

The capstone's arithmetic — 10^10 gates at 1,190–2,150 B/element is ~15 TB and
fits nowhere; a flat off-heap representation at ~100 B/element puts it near
~1 TB — **is correct and is not yet the binding constraint.** The ordering the
measurements support, and which HEAD's own commit message already reflects:

1. **`finishLoad`'s O(W) membership test** (FEAT-005). Two lines, 752×, measured,
   determinism preserved. Nothing else moves the reachable size by two orders of
   magnitude for two lines.
2. **Iterative `longestPath`** — unblocks the import path at all, and is a
   prerequisite for any corpus-driven testing. The memo map `layer` and the cycle
   guard `onStack` already exist; only the call form changes.
3. **An import entry point** — without it §5.0 is untestable by a user and the
   corpus cannot be exercised at all.
4. **The Splitter/Binder mesh, plus `$dff`/`$add`/`$mem`/reduction realization** —
   unblocks *real* netlists, which is a different problem from unblocking *large*
   ones. The largest single piece of work on the list, and the one that decides
   whether any artifact in §2 ever enters JLS.
5. **The 64 MiB cap** — a **security** control (issue #38, live attacks in
   `SECURITY.md`). Raising it is a policy decision with a threat model attached.
   The shape of the fix is a trusted-source opt-in, not a bigger constant.
6. **The flat off-heap representation** (FEAT-054) — CAP-17's actual subject.
   The ladder says where it first becomes the *binding* constraint: **rung 14**,
   at 7.6–8.2 × 10^6 elements, where 8.2 M × 1,190 B = 9.8 GB live exceeds
   default max heap on any commodity machine.

**And the finding from probe 2 that reframes the 10^10 question entirely.**
Getting an enormous netlist into JLS's import format is not the bottleneck and
never was. The 25-tile OpenPiton chip converts to a 1.47 GiB hierarchy-preserving
Yosys JSON in 1 m 51 s; the flat equivalent at the measured 576 B/cell would be
~45 GB, and at 10^10 gates ~5.8 TB — unparseable and unstorable. Concretely:
**78,693,320 leaf cells = 94–169 GB of JLS live heap if flattened, versus
~3.7–6.8 GB if the 25 tiles share one definition.** So the decision that gates
the whole capacity axis is whether JLS stores a design as
**definitions-plus-instantiations or as 10^10 live objects** — and JLS already
has nested subcircuits. That is a question about the element model, and it is
decided long before any distribution machinery matters.

### 5.4 Two incidental defects, neither about scale, both cheap

- **`finishLoad`'s OOM handler allocates and therefore itself OOMs.** The
  `-Xmx256m` run threw from `Circuit.java:1412`, *inside* the `catch (Error er)`
  block, so the user gets a raw stack trace instead of the intended message.
- **`FileAbstractor.writeCircuit` has no size cap while `openCircuit` does.**
  MEASURED: JLS wrote a **469,408-byte** `.jls` that it then refused to open,
  because it decompresses to 64.3 MiB. **A size limit that becomes silent data
  loss** for anyone who saves a large circuit and closes the editor.

---

## 6. WHAT TO DO NEXT WEEK — THE CHEAPEST EXPERIMENT

**Measure the import ratio on one real Rocket config, and get the `$adff`
histogram.** It is the only experiment that produces a number nobody has, on the
one artifact that is simultaneously large, permissively licensed, RISC-V,
Linux-capable and obtainable without a Chisel build. Everything else on the
ladder is either already measured or gated on work that has not happened.

It costs **one 721 MB clone and about twenty minutes of Yosys**, needs no Scala,
no sbt, no FIRRTL, no vendor tools, and it answers three open questions at once:
(a) what the *real* elements-per-word-cell ratio is on a design of Rocket's shape
rather than the DERIVED 1.80; (b) exactly how many `$adff` a Rocket import would
have to reject; (c) what the wire-end count is, which is the only quantity
`finishLoad` cost depends on and the one this study has never measured on a real
netlist.

```bash
# 1. The artifact. Pre-generated Verilog, Apache-2.0, no Chisel build.
git clone --depth 1 https://github.com/litex-hub/pythondata-cpu-rocket rk
#    721 MB. One config's .v is 9.5-12.7 MB.

# 2. Two stubs Yosys needs before it will elaborate Rocket, and the
#    companion SRAM file. Without .behav_srams.v the *_ext wrappers are
#    unresolved; both stubs are recorded at scratchpad/vhw/synth/stubs.v.
cat > stubs.v <<'EOF'
module plusarg_reader #(parameter FORMAT="", DEFAULT=0, WIDTH=1)
  (output [WIDTH-1:0] out); assign out = DEFAULT; endmodule
module EICG_wrapper (output out, input en, input test_en, input in);
  assign out = in & (en | test_en); endmodule
EOF

# 3. Word-level, MEMORIES PRESERVED. `memory -nomap` is the load-bearing
#    flag: it keeps each RAM as one $mem_v2 -> one JLS Memory element.
#    NOTE: bare `synth` would flatten memories. Do not use it here.
cat > rk.ys <<'EOF'
read_verilog stubs.v
read_verilog rk/pythondata_cpu_rocket/verilog/LitexConfig_linux_1_1.v
read_verilog rk/pythondata_cpu_rocket/verilog/LitexConfig_linux_1_1.behav_srams.v
hierarchy -check -top ExampleRocketSystem
proc; opt_clean; memory -nomap; wreduce -memx; opt; dffunmap; pmuxtree
flatten; opt_clean
stat
select t:$adff %% ; stat        # the async-reset blocker, counted
select t:$mem_v2 ; dump -n      # every memory, SIZE x WIDTH
write_json rk_linux_1_1.json
EOF
yosys -q rk.ys | tee rk_linux_1_1.word.txt
# expect ~41,733 word cells, 351 $mem_v2, 290,569 bits, 123 $adff

# 4. Sanity-check the mapper against the study's own anchor BEFORE
#    trusting anything it says about Rocket. This must print 724 -> 1598.
python3 scratchpad/vhw/synth/jlsmap.py scratchpad/vhw/synth/vexMin.word.txt

# 5. Then the number nobody has: run the netlist through CellValidator and
#    NetlistImporter and count problems by kind, exactly as was done for
#    PicoRV32 (947 problems on 681 cells).
CP=/home/user/JLS/target/jls-5.0.5-SNAPSHOT.jar:.
java -Xss4m -Xmx8g -cp $CP Imp rk_linux_1_1.json
#    -Xss4m because WALL A is a stack overflow at combinational depth
#    4,000-5,000 and Rocket is deeper than the bc4000 probe.
```

**What each outcome would mean.** If the problem histogram is dominated by
bit-slice/concat as PicoRV32's was (524 of 947), the Splitter/Binder mesh is
confirmed as the single blocking item and the ratio between it and cell
realization is now known at two scales instead of one. If `$adff` comes back at
123 as probe 1 measured, the async-reset lowering rule has a hard, countable
cost attached to it for the first time. If the run does not reach the importer at
all because `-Xss4m` is insufficient, that is itself the WALL A measurement on a
real design rather than on a synthetic chain — which is a number this study does
not have.

**The one-line version, if there is time for only one command:** run step 3
alone. It produces the `$adff` count, the memory inventory with the flag stated,
and a Yosys JSON that every subsequent experiment needs.

---

## 7. CAUTIONS

1. **Every count in this document carries a memory-handling convention and they
   are all different.** Titan counts a 9 Kib M9K as **one** primitive; ORFS
   std-cell counts **exclude** SRAM macros; ITC'99 `.bench` has **no** memories;
   EPFL has **no state at all**. Cross-suite comparison without restating the
   flag produces nonsense.
2. **Do not estimate JLS elements from LUT counts.** SERV vs PicoRV32 is 7.3× in
   LUTs and 1.5× in JLS elements. The one LUT-derived figure here (XiangShan,
   10^7–10^8) is banded to an order of magnitude and will be wrong in **both**
   directions — its huge SRAM arrays consume BRAM not LUTs (pushing the true
   count down, since JLS collapses each RAM to one `Memory`), while its
   structural complexity per LUT is higher than VexRiscv's (pushing it up).
3. **The elements-per-word-cell ratio is not constant.** It falls monotonically
   with design size across all twelve measurements: 2.21 (VexRiscv_Min, 724
   cells) → 2.11 → 2.06 → 2.05 → 1.99 → 1.80 → 1.76 → 1.75 → **1.74** (Rocket
   `linux_8_1`, 265,582 cells). Big designs are `$mux`/`$or`/`$and`-heavy (1
   element each); small cores are `$eq`-heavy (4 each, decode against constants).
   Extrapolating a large design with the small-core 2.1 overestimates by ~25%.
   **Use 1.74–1.80 above ~10^4 cells, 2.0–2.2 below ~2 × 10^3.**
4. **The Rocket element counts in §2 are optimistic by construction.** `$adff`
   was costed at 2 elements (the sync-reset rewrite) — a fix that does not exist.
5. **Titan/Titanium licensing is the largest open legal question and it gates the
   best rungs above 10^6.** VTR's `LICENSE.md` says the benchmark circuits *"are
   all open source but each have their own individual terms and conditions which
   are listed in the source code of each benchmark"* — the terms exist, are
   per-circuit, and are inside a ~1 GB tarball on a host this study could not
   reach. Against that, VTR's own docs say Titanium benchmarks *"incorporate
   Intel/Altera-specific IPs"*. **Measuring against Titan is uncontroversial;
   shipping those netlists inside a JLS corpus is a separate act needing the
   per-file headers read first.** That check costs one 1 GB download and ten
   minutes.
6. **The licence register, because three entries are routinely mis-recorded.**
   CV32E40P and CVA6 are **Solderpad Hardware License v0.51**, an Apache-derived
   *hardware* licence that is **not** Apache-2.0. XiangShan is **Mulan PSL v2**.
   Rocket is **dual Apache-2.0 + BSD-3-Clause**. OpenPiton's scalable
   `tile`/`chip` artifacts contain OpenSPARC T1 and are **GPLv2** even though
   OpenPiton's own RTL is BSD-3. ITC'99 is **EUPL v1.2** per its README and
   `LICENSE`, but probe 5 could not cite terms for the distribution it used —
   treat any un-headed copy as unknown, not permissive. IWLS'05's *netlists* were
   mapped with Cadence RTL Compiler against a library "free for personal or
   classroom use" and are **not** obviously redistributable even where the RTL
   is. Cleanly permissive: VTR/EPFL (MIT), ORFS (BSD-3), Rocket, VexRiscv (MIT),
   PicoRV32/SERV (ISC), and the BSD-3 SI tooling.
7. **"Freely downloadable" is not "freely licensed", and for the IEEE channels
   that gap is the whole story.** IEEE 802 public areas need no login, but
   contributions remain their contributors' property. Any plan to put an `.s4p`
   in the JLS tree needs legal review first, and the CI-download route is not
   obviously blessed either.
8. **COM is a golden answer only when pinned.** The SI community itself reports
   *"certain differences between more recent versions of this code and the
   specification … can cause repeatability issues"*. Any conformance test must
   record the **COM code version AND the configuration workbook** — the
   frequency-domain analogue of this study's memory-handling flag. A COM value
   with no version and no config sheet is not a number.
9. **The open COM/AMI ecosystem has a bus factor of approximately one.**
   PyChOpMarg (alpha), PyBERT, ibisami and PyAMI are all David Banas. scikit-rf
   is the only broadly-maintained package in the stack.
10. **The smallest redistributable HF artifact with a golden answer is 5 Gb/s —
    8× below the 20 GHz point (Nyquist 2.5 GHz).** Using the SiSoft kit is honest
    only if the document also says that. It tests the **machinery** (convolution,
    FFE, `.ami` parsing, eye rendering, time base) and not the **regime**
    (band-limit, causality, passivity, mode conversion, crosstalk).
11. **The `finishLoad` extrapolation to the cap is a floor, not an estimate**
    (measured local exponent 3.71, not 2.0), and **the patched curve past 400,000
    wire ends is not established** — the 320k→800k step cost 7.3× for 2.5× size,
    and at those sizes GC and cache effects could not be separated from residual
    algorithmic cost (`stateHash` on the same run also went 1,160.9 → 5,740.3 ms,
    and that is not `finishLoad`).
12. **Every JLS measurement here uses synthetic chain circuits with exactly two
    wire ends per net.** Real netlists have high-fanout nets that lengthen
    `finishLoad`'s inner visit loop. The `ends.remove` quadratic is fan-out
    independent so the patch's benefit is safe, but absolute timings on real
    circuits are not established. **No real RISC-V core has ever been measured
    through the whole JLS pipeline, because no real core imports** — which is
    exactly what §6 proposes to change.
13. **Seven of the twelve core artifacts could not be synthesised in this study,
    and the reasons are tooling, not physics.** NEORV32 is VHDL with no GHDL
    plugin (`/usr/lib/yosys/` is empty). Ibex, CV32E40P and CVA6 are
    SystemVerilog with no `sv2v`, no slang plugin, no verilator. BOOM and
    XiangShan are Chisel with no pre-generated netlist published. **Installing
    `sv2v` alone would unlock three of them.**
14. **The XiangShan 16-core figures are vendor-reported and could not be fetched
    directly.** All four carrying hosts (univista-isg.com, eetrend.com,
    icsmart.cn, doit.com.cn) were proxy-denied; the figures come from
    search-index summaries that agree across all four. Treat as a real,
    consistent press-release number that has not been independently reproduced.
    The same applies to the `802-COM` repository's existence and licence, the
    exact IEEE public-area terms, and the 802.3ck/dj tools-page contents —
    **all flagged for verification by a human with unrestricted network access.**
15. **Two small upstream drifts between this study's earlier pass and today's
    clones**, reported rather than smoothed over: picorv32 measures 681 word
    cells today against the study's 733 (−7.1%); VexRiscv_Linux measures 2,448
    against 2,492 (−1.8%). Same pipeline, different fetch dates. Neither changes
    a conclusion, but a re-run should expect the anchors to move by single-digit
    percent.
16. **Maintenance is not uniform, and the study's own calibration anchor is the
    stalest artifact on the list.** Last commits, measured by fresh shallow
    clone: NEORV32 2026-08-01, picorv32 2026-07-31, XiangShan 2026-07-31, CVA6
    2026-07-30, Ibex 2026-07-28, BOOM 2026-07-22, SERV 2026-07-10, Rocket
    2026-06-02, CV32E40P 2026-04-17, **VexRiscv 2026-02-11 (~6 months)**. OPDB
    itself is dormant since 2023-03-06 (complete, but not moving); OpenSERDES is
    frozen since 2022-03-26.
17. **D10.** This is a survey of what exists and what breaks first. Nothing here
    argues that CAP-17 should be funded, that Rocket should be adopted as the
    corpus, that the async-reset defect should be fixed, or that a synthetic
    generator should be built. §3 is a description of what an honest stressor
    would have to be, not a recommendation to make one. The one judgement
    defended is narrower and purely factual: **the real corpus is not the stress
    test, it is the calibration standard for the stress test — and it exists at
    10^5, 10^6 and 10^7, which is exactly where a generator can be validated
    before being trusted at 10^9 where nothing real exists.**

---

## Appendix — the URL register

Every artifact named above, with the licence as read rather than as assumed.

| Artifact | URL | Licence, as read |
|---|---|---|
| SERV | <https://github.com/olofk/serv> | ISC |
| PicoRV32 | <https://github.com/YosysHQ/picorv32> | ISC (`COPYING`) |
| NEORV32 | <https://github.com/stnolting/neorv32> | BSD-3-Clause |
| VexRiscv | <https://github.com/SpinalHDL/VexRiscv>; netlists <https://github.com/litex-hub/pythondata-cpu-vexriscv> | MIT |
| Ibex | <https://github.com/lowRISC/ibex>; area <https://carrv.github.io/2021/papers/CARRV2021_paper_8_Gallmann.pdf> | Apache-2.0 |
| CV32E40P | <https://github.com/openhwgroup/cv32e40p>; <https://docs.openhwgroup.org/projects/cv32e40p-user-manual/> | **SHL-0.51** |
| CVA6/Ariane | <https://github.com/openhwgroup/cva6>; 1.7 GHz result <https://www.semanticscholar.org/paper/814e538b8c3505553c8840cc5a201a6d5a1b0ada> | **SHL-0.51** |
| Rocket Chip | <https://github.com/chipsalliance/rocket-chip>; netlists <https://github.com/litex-hub/pythondata-cpu-rocket> | Apache-2.0 + BSD-3 |
| BOOM | <https://github.com/riscv-boom/riscv-boom>; <https://carrv.github.io/2020/papers/CARRV2020_paper_15_Zhao.pdf> | BSD-3 |
| XiangShan | <https://github.com/OpenXiangShan/XiangShan>; <https://github.com/OpenXiangShan/XiangShan/issues/3638>; <https://ieeexplore.ieee.org/document/9923860> | **Mulan PSL v2** |
| XiangShan 16-core FPGA result | <https://www.univista-isg.com/site/news_detail/422>, <https://www.eetrend.com/content/2025/100590349.html>, <https://www.icsmart.cn/90582/>, <https://www.doit.com.cn/p/532967.html> | vendor release |
| AMD VU19P product brief | <https://www.xilinx.com/content/dam/xilinx/publications/product-briefs/virtex-ultrascale-plus-vu19p-product-brief.pdf> | vendor |
| OpenPiton / OPDB | <https://github.com/PrincetonUniversity/openpiton>, <https://github.com/PrincetonUniversity/OPDB> | BSD-3 own RTL; **GPLv2** for `tile`/`chip` (OpenSPARC T1) |
| Chipyard | <https://github.com/ucb-bar/chipyard> | BSD-3 |
| Constellation NoC | <https://github.com/ucb-bar/constellation>; <https://chipyard.readthedocs.io/en/stable/Generators/Constellation.html> | BSD-3 |
| LiteX | <https://github.com/enjoy-digital/litex> | BSD-2 |
| ESP | <https://github.com/sld-columbia/esp> | Apache-2.0 + **GPLv2** (GRLIB) + LGPL-3 |
| OpenTitan | <https://github.com/lowRISC/opentitan>; <https://opentitan.org/book/hw/top_earlgrey/doc/datasheet.html> | Apache-2.0 |
| BlackParrot | <https://github.com/black-parrot/black-parrot> | BSD-3 |
| VTR / Titan / Titanium | <https://github.com/verilog-to-routing/vtr-verilog-to-routing>; mirror `https://www.eecg.utoronto.ca/~vaughn/titan/` | MIT **with benchmarks carved out**; per-circuit terms **not public to this study** |
| OpenROAD-flow-scripts | <https://github.com/The-OpenROAD-Project/OpenROAD-flow-scripts> | BSD-3; per-design upstream terms |
| EPFL benchmarks | <https://github.com/lsils/benchmarks>; MtM at <https://zenodo.org/record/2572934> | MIT (repo); Zenodo terms **unverified** |
| ITC'99 | <https://github.com/cad-polito-it/I99T> | EUPL v1.2 (README + `LICENSE`) |
| ISPRAS aggregator | <https://github.com/ispras/hdl-benchmarks> | per-suite upstream |
| ANG / artnetgen | <https://github.com/daeyeon22/artificial_netlist_generator> | **GPL-3** |
| gnl | <https://users.elis.ugent.be/~dstrooba/gnl/> | **not public to this study** (403) |
| IBIS-AMI test kits | <https://github.com/IBIS-Library/IBIS-AMI-test-kits> | per-ZIP: SiSoft permissive, **Cadence proprietary** |
| PyChOpMarg | <https://github.com/capn-freako/PyChOpMarg> | BSD-3 |
| PyBERT / ibisami / PyAMI | <https://github.com/capn-freako/PyBERT>, `/ibisami`, `/PyAMI` | BSD-3 |
| scikit-rf | `pip install scikit-rf` | BSD-3 |
| IEEE 802-COM | `https://opensource.ieee.org/802-com/com_code`; <https://opensource.ieee.org/community/cla/bsd3> | BSD-3 **[verify — host blocked]** |
| IEEE 802.3 channel archives | <https://grouper.ieee.org/groups/802/3/ap/public/channel_model/archive/index.html>, <https://www.ieee802.org/3/ck/public/tools/index.html>, <https://www.ieee802.org/3/dj/public/tools/index.html> | **not redistributable** |
| OpenSERDES | <https://github.com/SparcLab/OpenSERDES> | **GPL-3** |
| AIB PHY hardware | <https://github.com/chipsalliance/aib-phy-hardware> | Apache-2.0 |
| OIF CEI IAs | <https://www.oiforum.com/technical-work/implementation-agreements-ias/> | permissive copy notice on the IA PDFs |
| `vectfit3.m` | <https://www.sintef.no/en/software/vector-fitting/> | **NOT open — must not be absorbed** |
