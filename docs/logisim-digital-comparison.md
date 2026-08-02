# Logisim-evolution and Digital as migration targets for JLS

*A scope-and-migration survey, August 2026. Sibling to
[`docs/kicad-ngspice-comparison.md`](kicad-ngspice-comparison.md), which
answers the same question for a tool in a different class and concludes
that these two are the targets worth pricing instead. Builds on the
verified peer research already in
[`docs/hdl-support-research.md`](hdl-support-research.md) §2 and §7.1 and
scattered findings in [`docs/capability-roadmap/`](capability-roadmap/).*

## 0. The question, and the short answer

**Is either of the de facto open-source standards — Logisim-evolution or
hneemann's Digital — approximately a superset of JLS, and what would a
migration path cost?**

**Approximately, with real holes, and the holes are not where the element
lists suggest.** Both tools are in JLS's own class: schematic-first,
Java, single-jar, bus-valued signals, hierarchical subcircuits, HDL
export, headless test CLIs. On raw capability both are *ahead* of JLS in
several dimensions JLS's own roadmap wants (per-bit high-Z, FPGA board
databases, parameterized circuits, larger libraries, an active team). The
"is it a superset" question therefore comes down to a much shorter list
than it did for KiCad:

| | Logisim-evolution | Digital |
|---|---|---|
| **Superset of JLS's infrastructure?** | yes, and then some | yes, and then some |
| **Superset of JLS's element vocabulary?** | **no** — no FSM element, no placeable truth table | **nearly** — has both, but as *synthesis tools*, not simulated elements |
| **Superset of JLS's simulation semantics?** | **no** — fixed component delays | **no** — unit delay; the Delay escape hatch caps at 20 |
| **Superset of JLS's value domain?** | **no** — 64-bit cap (JLS is unbounded) | **no** — 64-bit cap |
| **Superset of JLS's grading contract?** | stronger CLI, **no stability promise** | comparable CLI, **no stability promise** |

Four findings do most of the work, and all four are structural rather
than cosmetic:

1. **Both cap bit width at 64.** `Value.MAX_WIDTH = 64` in
   Logisim-evolution; Digital stores a value as `long value` +
   `long highZ`. JLS uses `java.util.BitSet` with no cap — the register
   dialog rejects only `bits < 1` (`src/jls/edit/RegisterDialog.java:357`).
   Any JLS circuit with a wider-than-64-bit signal has no target.
2. **Neither reproduces JLS's delay model.** JLS gives every element a
   per-instance, user-editable integer transport delay (AND 10, NAND 5,
   Mux 25, Register 50, Memory 100, Adder 30×bits). Digital gives *every*
   gate the same unmodifiable propagation time and offers a Delay
   component whose value maxes out at 20. Logisim's per-component delays
   are fixed and, in its own documentation, "somewhat arbitrary."
3. **Digital's file format has no element identity at all** — identity is
   `<pos x= y=>` and connectivity is coordinate coincidence (verified by
   direct fetch, recorded at
   `capability-roadmap/lf-06-diff-merge-vcs.md:650`). Converting into it
   discards JLS's stable ids and everything built on them.
4. **The grading interface is where the userbase actually lives, and it
   does not convert.** Both peers have headless test CLIs — Logisim's is
   arguably better than JLS's — but JLS's `-t` stimulus is *time-based*
   in abstract units while both peers' test formats are *row-based*. A
   migration rewrites every autograder in the userbase regardless of how
   good the circuit converter is.

The recommendation (§6): **build a one-way `.jls` → `.dig` exporter, not
a migration** — architecturally a third printer beside the Verilog and
VHDL emitters — and ship it with a **differential harness** that
certifies each converted circuit by re-simulating it. Without the
harness the converter is not trustworthy enough for an instructor to
stake a course on, and JLS is unusually well-placed to build one.

---

## 1. Head to head

Every JLS row is anchored in the tree or its normative docs. Peer rows
are marked **[v]** verified this pass or in-repo, **[s]** secondary
source, **[u]** unverified.

### 1.1 Simulation model

| | JLS | Logisim-evolution | Digital |
|---|---|---|---|
| Value states | 2-state + whole-signal HiZ (`simulation-semantics.md` §2) | **4-state per bit**: value/unknown/error masks **[v]** | **2-state + per-bit high-Z** (`long value`, `long highZ`) **[v]** |
| Max bit width | **unbounded** (`BitSet`; only `bits < 1` rejected) | **64** (`Value.MAX_WIDTH`) **[v]** | **64** (`long`) **[v]** |
| Time model | dimensionless 64-bit integer, discrete-event | step + tick; delays fixed per component **[s]** | **unit delay**, synchronous batch update **[v]** |
| Per-instance delay | **yes**, user-editable, documented defaults + global reset | no; fixed per component, "somewhat arbitrary" **[s]**; a separate Delay component exists **[s]** | no; all gates identical, "cannot be changed"; Delay component **capped at 20** **[s]** |
| Delay discipline | transport (narrow pulses survive) | unspecified **[u]** | synchronous update; oscillation detection **[v]** |
| Multi-driver | deterministic (first active driver in net order), warns once | ERROR state on the bit **[v]** | undefined value into gate inputs **[s]** |
| Determinism contract | **published and golden-tested** (canonical stable-id seed order, issue #181) | none published **[u]** | none published **[u]** |
| Normative semantics spec | **yes** (`docs/simulation-semantics.md`) | no **[u]** | no **[u]** |

The two rows that matter most are **max bit width** and **per-instance
delay**, and they cut in opposite directions from what one would guess:
the peers are ahead on the *value* model (per-bit HiZ, 4-state — both
things `capability-roadmap/sweep-01` wants for JLS) and behind on the
*timing* model.

### 1.2 Element vocabulary

Mapping JLS's 34 registered types (`src/jls/elem/ElementRegistry.java`):

| JLS element | Logisim-evolution | Digital |
|---|---|---|
| gates, TriState, Constant, Clock | direct | direct |
| `Splitter` / `Binder` | Splitter (direct) | Splitter (direct) |
| `JumpStart` / `JumpEnd` | Tunnel (direct) | Tunnel (direct) |
| `SubCircuit` | subcircuit (direct) | subcircuit (direct), **plus generics JLS lacks** **[v]** |
| `Adder`, `Mux`, `Decoder`, `ShiftRegister`, `Register`, `RegisterFile`, `Extend` | direct (arithmetic/plexer/memory libs) | direct (74xx + built-ins) |
| `Memory` (RAM/ROM + contents editor) | RAM/ROM with contents editing **[v]** | RAM/ROM/EEPROM with contents editing **[s]** |
| `Display` | hex digit / 7-segment | 7-segment / LED |
| `SigGen` | test vector / input pin driving | in-editor test case |
| `TruthTable` **(placeable, simulated, delay 30)** | **none** — Combinational Analysis is a synthesis *tool* | **none as an element** — analysis/synthesis via Quine-McCluskey **[v]** |
| `StateMachine` **(placeable, simulated, Moore outputs, busy semantics, warn-once unmatched edge)** | **none** | **FSM editor that converts to a state table and a circuit** **[v]** — a *generator*, not an element |
| `Stop` / `Pause` (circuit halts the run) | **none** | **none** |
| `DelayGate` (arbitrary user delay) | Delay component **[s]** | Delay component, **max 20** **[s]** |
| `FieldExtend`, `Text`, `TestGen` | partial / annotation | partial / annotation |

The `TruthTable` and `StateMachine` rows are the interesting ones and
they are the same distinction twice: **both peers can *build a circuit
from* a truth table or state diagram; only JLS *simulates the table or
diagram itself* as a first-class element with its own propagation
delay.** `docs/hdl-support-research.md:294` already records this as an
area where JLS is "*ahead* of some contemporaries."

For a migration this is not a blocker but it is a lossy transform: a JLS
`StateMachine` converts to a synthesized gate network, and the student's
state diagram — the artifact the assignment was about — stops being the
runtime object. Round-tripping is gone.

### 1.3 Everything around the circuit

| | JLS | Logisim-evolution | Digital |
|---|---|---|---|
| HDL export | Verilog-2005 + VHDL-93 (`std_logic_1164`, `numeric_std`) | VHDL + Verilog, own per-component generator framework **[v]** | VHDL + Verilog **[v]** |
| HDL import | Yosys JSON netlist + auto-layout | none general **[v]** | components described in VHDL/Verilog, co-simulated via **GHDL / Icarus** **[v]** |
| Embedded-HDL co-sim | roadmap (#63) | VHDL only, needs **Questa/ModelSim**; UNKNOWN without it **[v]** | GHDL / Icarus — the better model **[v]** |
| FPGA boards | iCE40 PCF emission (#213/#215) | **29 built-in board XMLs**, 4 toolchains, pin-mapping GUI **[v]** | BASYS3, TinyFPGA BX; JEDEC for GAL16v8/22v10/ATF150x **[v]** |
| Headless test CLI | `-b -t`, watched-output report, exit 0/1/2 | `--test-vector`, `--tty` csv/table, `--substitute`, `--test-fpga HDLONLY` **[v]** | `java -cp Digital.jar CLI test <file> [-tests <file>]` **[v]** |
| Test format | **time-based** (`for d v` / `until t v`, abstract units) | **row-based CSV** with don't-cares, HiZ, sequential mode **[v]** | **row/DSL in a drawable element**, clock column **[s]** |
| Grading stability promise | **yes, explicit** (`batch-interface.md`) | none published **[u]** | none published **[u]** |
| VCD export | **yes**, byte-deterministic, golden-tested | not found **[u — searched, no evidence]** | not found **[u — searched, no evidence]** |
| Waveforms | trace window + VCD → GTKWave/Surfer | chronogram, PNG/SVG export **[v]** | measurement graph **[s]** |
| Image export | PNG/JPEG/**SVG** headless | SVG/PNG of timing diagram **[v]** | SVG incl. LaTeX/Inkscape variant **[v]** |
| Parameterized subcircuits | **no** | **no** (verified: no parameter attribute in `CircuitAttributes.java`) **[v]** | **yes**, generic circuits **[v]** |
| Element identity in save file | **stable ids** (#165/#181), canonical save | `.circ` XML; per-component ids **[u]** | `.dig` XML, **no element identifier**; identity = position **[v]** |
| Collaborative editing | P2P, in progress (#163) | none | none |
| Remote/programmatic API | none (roadmap) | TCL/TK console **[v]** | **remote TCP interface** **[v]** |
| Reproducible build + attestation | yes (jar/BOM, SLSA, cosign) | not claimed **[u]** | not claimed **[u]** |
| Container image for autograding | `ghcr.io/anadon/jls`, multi-arch incl. riscv64 | not found **[u]** | not found **[u]** |
| 74xx parts library | none | yes **[v]** | yes **[v]** |
| SoC / peripherals | none | DMA engine, FP16, terminals, keyboard, joystick **[v]** | terminal, keyboard, graphics, external processor **[s]** |
| i18n | none (deliberate) | multi-language **[v]** | 7 languages **[v]** |
| Maintenance | single maintainer | active team, 4.1.0 Feb 2026 **[v]** | active single maintainer, large user base **[v]** |

---

## 2. Where each is genuinely a superset

**Logisim-evolution** is a superset of JLS in: FPGA reach (29 boards, four
toolchains including an open-source GHDL→Yosys→nextpnr→openFPGALoader
flow), component breadth (SoC components, DMA, FP16, I/O devices), the
headless grading CLI (`--substitute` for grading against a reference
implementation is a feature JLS does not have and should want), 4-state
per-bit values, the chronogram, TCL/TK scripting, i18n, and institutional
momentum.

**Digital** is a superset of JLS in: per-bit high-Z, generic
(parameterized) circuits, circuit analysis and synthesis (Quine-McCluskey
minimization, combinational *and* sequential), the FSM editor, the 74xx
library, HDL co-simulation via GHDL/Icarus (the pattern
`hdl-support-research.md` already recommends JLS adopt), the remote TCP
interface, JEDEC export for real GAL/ATF chips, and simulation
performance.

Both are supersets in the dimension that prompted the question: they are
maintained by people who are not one person.

---

## 3. What JLS still holds that neither has

Consolidated from §1, in migration-debt order:

1. **Unbounded bit width.** Hard structural cap of 64 on both sides.
2. **A per-instance, editable, documented delay model** — and with it,
   every lab that teaches hazards, races, or critical paths by dialing a
   delay. Digital's escape hatch caps at 20; JLS's *default* Register
   delay is 50, Memory 100, and Adder is 30×bits (a 4-bit adder is 120).
   A uniform rescale of JLS's gate/register/memory defaults (5, 10, 25,
   30, 50, 100 — gcd 5) does fit under 20, but the width-scaled Adder and
   any user-customized delay do not.
3. **The truth table and the state diagram as simulated elements**,
   round-trippable, rather than as one-way synthesis inputs.
4. **In-drawing simulation control** (`Stop`, `Pause`).
5. **A published normative simulation semantics** with golden tests, and
   a **published stability contract** for the grading interface. Neither
   peer promises either. For a course that has built an autograder, this
   is the single most valuable thing JLS offers and the thing migration
   most directly gives up.
6. **Byte-deterministic VCD export** into the standard waveform
   ecosystem. Neither peer appears to emit VCD at all.
7. **Stable element identity in the save file** (and therefore the whole
   semantic-diff/merge and P2P-collaboration line of work). Digital's
   format cannot express it; Logisim's is unverified but its own
   community advice is "work on a single computer at a time."
8. **Reproducible, attested builds and a multi-arch autograding
   container**, including riscv64.
9. **The `riscv/` programmatic circuit-construction toolchain** — Python
   that emits `.jls` and differentially fuzzes the result. Neither peer
   is known to expose a construction API
   (`capability-roadmap/lf-07-api-and-platform.md:471`, marked unverified
   there and still unverified here).

Items 1–4 are *circuit* losses a converter must confront. Items 5–9 are
*ecosystem* losses no converter touches.

---

## 4. The migration path, staged

### Stage 0 — pick the target

**Recommend Digital**, on four grounds: the element vocabulary is the
closest match (FSM editor, memory with contents, 74xx, generics); its
HDL philosophy is identical to the one JLS already adopted from it
(`hdl-support-research.md` §2, "deployment vehicle, not HDL tutorial");
its test-case model — a table plus a small DSL inside a drawable element
— is what `capability-roadmap/sweep-04` already identifies as the shape
JLS's own test surface should move toward; and it is the tool JLS's
research corpus already treats as the reference point.

**The decisive argument against Digital**, stated plainly: `.dig` has no
element identifier. If the stable-id / semantic-merge / collaboration
line of work matters, Digital cannot receive it, and Logisim-evolution
(`.circ`, per-component ids unverified) is the only candidate that might.

Choose Logisim-evolution instead if the deciding factor is institutional
install base in US CS courses, FPGA board coverage, or the `--substitute`
grading workflow.

### Stage 1 — the mechanical half (cheap, and not the risk)

`.jls` is a documented line-oriented text format (`docs/file-format.md`)
and both targets are XML. A converter walks the loaded `Circuit` and
emits the target tree: elements, positions, wire nets, hierarchy
(`SubCircuit` → nested `.dig`/`.circ`), text annotations, watched flags.

**The one real hazard here is Digital's coordinate-implied
connectivity.** Digital has no netlist; two things are connected because
their coordinates coincide. A converter that gets JLS's grid mapping
subtly wrong produces a file that *loads* and is silently miswired. The
mitigation is not review, it is Stage 3.

### Stage 2 — element mapping

A per-type mapping table with three buckets, from §1.2:

- **Direct** (~20 of 34 types): gates, TriState, Constant, Clock,
  splitter/binder, tunnels, subcircuit, adder, mux, decoder, shifter,
  register, register file, memory, displays, pins.
- **Lossy / synthesized** (4): `TruthTable` and `StateMachine` become
  generated gate networks (Digital can generate them; Logisim-evolution
  only for the combinational case); `DelayGate` becomes a capped Delay
  component; `Extend`/`FieldExtend` become explicit wiring.
- **No target** (3): `Stop`, `Pause`, and any signal wider than 64 bits.
  These must fail the conversion loudly, not silently — a converter that
  drops a `Stop` element produces a circuit that runs to the time limit
  instead of halting, and nothing in the output says so.

### Stage 3 — semantics reconciliation, and the differential harness

This is the stage that decides whether the migration is trustworthy, and
it is the one a naive converter skips.

The delay-model mismatch (§3 item 2) means **conversion is not
behavior-preserving in general**. Three honest options, in preference
order:

- **(a) Certify per circuit, don't promise in general.** Convert, then
  run the JLS original under `-b -t` and the converted circuit under the
  target's CLI, and diff the watched outputs. Circuits that agree are
  certified; circuits that diverge are reported with the diverging
  signal and timestamp. This is the only option that produces a claim an
  instructor can rely on.
- **(b) Rescale delays where the ratios fit.** Divide JLS's delays by
  their gcd and emit Delay components. Works for the default gate /
  register / memory set; fails on width-scaled Adder delays and on
  customized values. Useful as an optimization *inside* (a), never as a
  substitute for it.
- **(c) Drop delays and document the divergence class.** Cheapest,
  and acceptable only for circuits whose outputs are delay-independent —
  which is exactly the property (a) establishes.

**JLS is unusually well-positioned to build (a)**, and this is the
strongest technical argument in the whole document. It already has: a
normative semantics spec, a golden test suite pinning it
(`BatchSimulationGoldenTest`, `SequentialGoldenTest`,
`VcdExportGoldenTest`), a headless simulator with a machine-readable
output contract, and a *working precedent for differential fuzzing* in
`riscv/fuzz_diff.py`, which already fuzzes a drawn CPU against a
reference emulator. A `.jls`→`.dig` differential harness is the same
pattern pointed at a different oracle.

Other reconciliations, all smaller:

- **HiZ**: whole-signal → per-bit is a widening; safe.
- **Multi-driver conflicts**: JLS resolves deterministically and warns;
  Logisim yields ERROR, Digital yields undefined. Behavior changes — but
  only for circuits JLS already warns about, so arguably a fix. Surface
  it in the conversion report either way.
- **Time limits and termination reasons**: no equivalent; the converted
  circuit's run ends differently. Matters for grading, not for drawing.

### Stage 4 — the grading interface (the part the userbase actually uses)

Circuits are not what courses depend on; autograders are. Two
translations are needed, and only one of them is mechanical:

- **Stimulus.** JLS `-t` is time-based in abstract units
  (`a 0 for 10 1 until 30 0 end`). Both targets are row-based — Logisim
  CSV vectors, Digital's in-element table/DSL with a clock column.
  **There is no general translation** without choosing a sampling
  discipline, and choosing one re-imports the delay problem from Stage 3:
  where you sample depends on when the circuit has settled. For
  clocked circuits with a settled-between-edges discipline this is
  routine; for combinational circuits driven at arbitrary times it is
  not.
- **Results.** JLS's watched-element report and outcome line vs.
  Logisim's `--tty table/csv` or Digital's test pass/fail exit. This is
  mechanical, and it is a rewrite of every grading script regardless.

Sequence matters: a converter without Stage 4 moves the files and
strands the courses.

### Stage 5 — what stays behind regardless

§3 items 5–9: the published contracts, VCD, stable identity, reproducible
builds, the container, and the programmatic construction toolchain.
State them in the migration notes rather than discovering them
afterwards.

---

## 5. Cost

Rough, in the same units `capability-roadmap` uses (maintainer-weeks),
and calibrated against the existing `VerilogEmitter` — which is the same
shape of work: walk the element graph, print a foreign format.

| Stage | Estimate |
|---|---|
| 1 — geometry/net/hierarchy emitter | 2–3 |
| 2 — element mapping table + synthesis for TruthTable/StateMachine | 4–6 |
| 3 — differential harness + certification report | 4–5 |
| 4 — stimulus translator + results translator | 3–4 |
| **Total for a trustworthy one-way exporter** | **13–18** |

Stage 1 alone — the thing that looks like the whole job — is under three
weeks and is worth approximately nothing without Stage 3.

---

## 6. The determination

**Neither Logisim-evolution nor Digital is a strict superset of JLS, but
Digital is close enough that a migration is a real engineering project
rather than a category error** — which is the substantive difference from
the KiCad answer, where the gap was architectural.

What each fails to contain is narrow and specific: unbounded bit width,
a per-instance editable delay model, the truth table and state diagram as
simulated round-trippable elements, in-drawing simulation control, and —
outside the circuit itself — the published semantics and grading
contracts, VCD, stable element identity, and the attested/containerized
distribution.

The recommendation:

- **Build `-export out.dig` as a one-way exporter**, sitting beside
  `-export out.v` and reusing the same emitter architecture
  (`src/jls/hdl/HdlEmitter` walk, third printer). Frame it exactly as
  hneemann frames HDL export and as JLS already adopted for its own:
  a **vehicle**, not a claim of equivalence.
- **Ship it with the differential harness or don't ship it.** The
  converter's value is entirely in the certification, and JLS has the
  golden tests, the normative spec, and the `riscv/fuzz_diff.py`
  precedent to build it.
- **Translate the grading interface in the same release**, or accept
  that the files moved and the courses did not.
- **Do not migrate the project.** The peers lead on values, boards,
  libraries and manpower; JLS leads on timing, contracts, identity,
  determinism, and reproducibility — and `capability-roadmap/AMENDMENT.md`
  already identifies four positions (exhaustive verification drawn on the
  schematic, a real schematic merge driver, fault simulation on the
  drawing, clock-domain identity) that **no tool in any of the three
  classes occupies**, JLS's peers explicitly included. An exporter gives
  users an exit without conceding those.

Stated in one line: **make it easy to leave, and then earn the decision
to stay.**

---

## 7. Verification notes

JLS claims are anchored in the tree at HEAD. Peer claims carry inline
marks; the load-bearing ones and their status:

- **Verified this pass by direct source fetch**: Logisim-evolution
  `Value.MAX_WIDTH = 64` and the four-state value/unknown/error mask
  representation; `BitWidth.MAXWIDTH = Value.MAX_WIDTH`; Digital's
  `ObservableValue` holding `long value` + `long highZ` (per-bit high-Z,
  64-bit ceiling).
- **Verified this pass from primary docs/repos**: Digital's FSM editor
  producing a state table and a circuit; Digital's analysis/synthesis via
  Quine-McCluskey; Digital's generic (parameterized) circuits; Digital's
  GHDL/Icarus co-simulation, BASYS3/TinyFPGA BX, JEDEC export, remote TCP
  interface, and CLI (`java -cp Digital.jar CLI test …`);
  Logisim-evolution 4.1.0 (Feb 2026), chronogram, VHDL components,
  TCL/TK console, `-t/--tty` and `-w/--test-vector`.
- **Verified in this repository's prior research** (re-used, not
  re-derived): Logisim-evolution's HDL generator framework, 29 board
  XMLs, four toolchains, Questa-only VHDL co-simulation, and test-vector
  CLI (`hdl-support-research.md` §7.1); Digital's `.dig` lacking element
  identifiers (`lf-06-diff-merge-vcs.md:650`); Logisim-evolution lacking
  subcircuit parameterization (`lf-01-parameterization.md:484`).
- **Secondary sources, worth re-checking before acting**: Digital's
  unit-delay synchronous update model and its Delay component's maximum
  of 20; Logisim's per-component delays being fixed and "somewhat
  arbitrary"; Digital's test-case DSL specifics.
- **Unverified negatives** — searched, no evidence found, *not* proven
  absent: VCD export in either tool; a published grading-format stability
  promise in either; a programmatic circuit-construction API in either;
  per-component stable ids in `.circ`. Any of these turning out to exist
  would improve the migration case and should be re-checked before this
  document is relied on.

Sources consulted:
[Digital repository and README](https://github.com/hneemann/Digital),
[Digital gate delays discussion](https://github.com/hneemann/Digital/discussions/1302),
[Digital Delay component discussion](https://github.com/hneemann/Digital/discussions/1122),
[Digital CLI test issue](https://github.com/hneemann/Digital/issues/353),
[Digital `ObservableValue.java`](https://github.com/hneemann/Digital/blob/master/src/main/java/de/neemann/digital/core/ObservableValue.java),
[Logisim-evolution repository](https://github.com/logisim-evolution/logisim-evolution),
[Logisim-evolution `Value.java`](https://github.com/logisim-evolution/logisim-evolution/blob/main/src/main/java/com/cburch/logisim/data/Value.java),
[Logisim-evolution CHANGES.md](https://github.com/logisim-evolution/logisim-evolution/blob/main/CHANGES.md),
[Logisim-evolution docs](https://github.com/logisim-evolution/logisim-evolution/blob/main/docs/docs.md),
[Logisim-evolution command discussion](https://github.com/logisim-evolution/logisim-evolution/discussions/2119),
[Logisim gate delays guide](https://cburch.com/logisim/docs/2.6.0/en/guide/prop/delays.html),
[Logisim command-line verification](https://cburch.com/logisim/docs/2.7/en/html/guide/verify/index.html),
[Logisim propagation shortcomings](https://cburch.com/logisim/docs/2.7/en/html/guide/prop/shortcome.html).
