## Value domain, logic modelling, and simulation semantics

*Sweep 01. Survey entries #25–#47, #66–#73, plus every entry anywhere in
`docs/standards-landscape.md` whose support is blocked by JLS's two-state-plus-HiZ
value model. Written under the capability-expansion frame: the question is not
"can JLS do this cheaply" but "what would JLS have to change, and what else does
that change unlock".*

The single finding this sweep exists to state: **JLS's value domain is the
narrowest waist in the whole program.** It is one type — `java.util.BitSet`, with
`null` overloaded to mean high-impedance — and it is the reason ~20 numbered
standards are unreachable, the reason the VHDL emitter has to write a comment
apologizing for itself, the reason the Yosys importer has a counter named
`coercedX`, and the reason a first-year student who builds a bus conflict in JLS
learns something that is not true of hardware. Every other sweep's items are
features. This one is the program.

---

### The blocked standards

`§2` and `§9` of `docs/simulation-semantics.md` are the normative statements of
the limit:

> "Bits are two-state: 0 or 1. There is **no unknown/X state anywhere in the
> simulator**" (`docs/simulation-semantics.md:46-49`)
> "High impedance … is represented by a **null** value reference"
> (`:50-54`)
> "HiZ is all-or-nothing per signal … There is no per-bit HiZ" (`:55-56`)
> "There is no wired-AND/OR and no conflict (X) state."
> (`:439-440`)

Everything below follows from those four sentences.

| # | Standard | What blocks JLS today (code) | Change that unblocks |
|---|---|---|---|
| **25** | IEEE 1076 **VHDL** | VHDL's core idiom is a *resolved* subtype with a resolution function over a multi-value type. JLS has neither: `WireNet.propagate` resolves by "first active driver in net order" (`src/jls/elem/WireNet.java:454-485`), which is not a resolution function — it is not commutative-associative over its inputs and cannot be expressed as one. | **V1** + **V2** |
| **26** | IEEE **1164 `std_logic_1164`** — marked **HAVE** in the survey | This is the flagship pretence. `VhdlEmitter` declares `use ieee.std_logic_1164.all` (`src/jls/hdl/VhdlEmitter.java:67`) and satisfies the type's full-coverage rule with `when others` (`:467-471`, `:575-580`, `:658`, `:690`) — over a **nine-value** type of which JLS models **two and a half**. The emitter documents the lie in its own generated header: `"JLS simulates two states plus HiZ: this design drives '0'/'1'/'Z', never 'X'."` (`:100-101`, class doc `:24-25`). The survey's HAVE mark is for *emitting syntactically valid 1164*, not for *modelling 1164*. | **V1** + **V4** |
| **27** | IEEE **1076.3 `numeric_std`** | Partially real. `std_match` and the `'-'` don't-care are used by the TruthTable template (`VhdlEmitter.java:549`) but only for **input** columns; output don't-cares are destroyed before they reach the emitter — `HdlModel.java:617` "Per output column: 0 or 1 (don't care **already lowered**)", lowered by `TruthTable.react`'s `"don't care becomes false"` (`src/jls/elem/TruthTable.java:1447-1449`). `TO_01`, `TO_X01`, `is_X` have no counterpart. | **V1** + **V7** |
| **30 / 32** | IEEE 1076.6 / IEC-IEEE **62142** RTL-synthesis subsets | The subsets' whole economic value is the **synthesis don't-care**: assigning `1'bx`/`'-'` to an output tells the optimizer "any value". JLS cannot express it in a value, so it lowers it to 0 in the simulator *and* in the export (same two cites as #27), handing synthesizers a fully specified function and forfeiting the optimization the standard exists to enable. | **V1** + **V7** |
| **31** | IEEE **1364 Verilog-2005** — marked **HAVE** (export) | Structural export is real; the *language's logic model* is not reachable. Verilog is four-state (`0 1 x z`) with an **8-level strength lattice** (`supply/strong/pull/large/weak/medium/small/highz`), independent `strength0`/`strength1` per driver, net types `wand`/`wor`/`triand`/`trior`/`tri0`/`tri1`/`trireg`, the `pullup`/`pulldown` primitives, and the `bufif0/1`, `notif0/1`, `nmos/pmos/cmos` switch-level primitives. JLS has one net kind, one driver kind, no strength, no `x`. `HdlNames.java:51` reserves the words `pullup`/`pulldown` — the emitter knows they exist and can never emit them. | **V1** + **V2** + **V3** |
| **33** | IEEE **1800 SystemVerilog** | The 1800 distinction that matters pedagogically is `logic` (four-state) vs `bit` (two-state) and the `'x`/`'z` fill literals. JLS is `bit`-only, so any imported design whose reset or unknown behaviour is the point arrives stripped of it. | **V1** |
| **43** | IEEE **91/91a-1991** logic symbols — the survey's #1 ranked item (§13.1) | The adoption doc has already had to carve the value model out of the conformance claim: `docs/standards-adoption/01-iec-ieee-symbols.md:44-46` excludes "no analog, no transmission gates, **no bidirectional/open-collector output qualifiers**, no signal-flow-direction reversal arrows. **JLS has no elements that need them.**" IEEE 91 §'s output qualifying symbols (open-collector ◇, open-emitter, three-state ▽) and the bidirectional-signal-flow arrow are *exactly* the part of the standard JLS must skip — and it skips them because of the value/driver model, not the renderer. The flagship symbol-conformance claim is being made with a hole in it. | **V3** + **V6** |
| **44** | **IEC 60617-12** | Same hole, same cause: 60617-12's open-collector/open-emitter and passive-pull qualifiers describe driver kinds JLS's `Output` cannot be. | **V3** |
| **45** | IEEE 315 / ANSI Y32.2 | Weakly blocked: the pull-up/pull-down **resistor** is a 315 symbol with no JLS element to attach it to. A drawable pull element (V3) gives it one. | **V3** |
| **47** | **WaveDrom / WaveJSON** | WaveJSON's wave alphabet is `0 1 x z = u d 2..9` — unknown, high-impedance, and pull-up/pull-down markers. JLS can populate three of those characters. A WaveDrom exporter today would emit a strict subset and misrepresent itself the way `07-waveform-formats.md:128-134` says an EVCD writer would. | **V1** + **V3** |
| **66** | **VCD**, IEEE 1364-2001 §18 — **HAVE** | Conformant, but as a strict subset by construction, and the subset is *pinned as a contract*: `VcdExportGoldenTest.vcdIsStructurallyWellFormedAndTwoStatePlusHiZ`. VCD's value alphabet is `0 1 x z` plus the 8-strength `$dumpports`-adjacent forms; `BatchSimulator` can emit `0`,`1`,`z` and `bz` only (`src/jls/sim/BatchSimulator.java:522-551`), and mixed vectors like `b1z0` "**cannot** [occur]" (`docs/batch-interface.md:298-304`). Every third-party VCD JLS *reads* (there is no reader) or that a student compares against GHDL/Icarus output will contain `x`. | **V1** |
| **67** | **Extended VCD (EVCD)** | Fully blocked, and already recorded as such under the preservation filter: `docs/standards-adoption/07-waveform-formats.md:108-134` — "**JLS has no strength model**… the `strength` identifier exists nowhere under `src/jls/sim/`… an EVCD writer would emit, for every record, a constant strength pair and a state character drawn from a two-element subset… a file that *looks* like it carries drive-strength and direction information and does not". That doc's own revisit trigger is stated as a *semantic* one: "**JLS gains a drive-strength value domain or a bidirectional pin element**" (`:150-151`). This sweep's V2 and V6 are precisely those two triggers. The recorded "do NOT do EVCD, ever, **under the current simulation semantics**" (`:572`) is not a rejection of EVCD; it is a correctly-scoped rejection of EVCD-without-a-strength-model. | **V2** + **V6** |
| **68** | **FST** | Mostly unblocked (it is a container), but FST carries four-state and nine-state value payloads natively; writing it from a two-state source wastes the format and reproduces #66's subset problem. | **V1** (quality, not gate) |
| **72** | **SAIF** | SAIF's per-signal records are `T0`/`T1`/`TX`/`TZ` — time spent in **each of four states** — plus toggle counts. Two of the four fields are permanently zero from a JLS trace. Power-estimation labs (a real CE2016/#277 topic) are unreachable. | **V1** |
| **22** | **I²C** (NXP) / SPI — survey **COULD**, Tier 1 | I²C is not "a bus JLS could draw"; it is *definitionally* an open-drain wired-AND bus with pull-up resistors, and its multi-master arbitration **is** wired-AND arbitration. JLS has no open-drain driver, no wired-AND net, and no pull element (`grep -rni "open.drain\|wired.and\|pull.up" src/` returns only the Verilog reserved-word list and a tutorial's HTML "pull-down list"). An I²C lab is not hard in JLS today; it is impossible. | **V3** |
| **75** | **Yosys JSON netlist import** — **HAVE** | The importer parses `x` and `z` bits faithfully (`src/jls/hdl/yosys/YosysNetlist.java:42-45`, `:889-893`) and then throws the information away: `NetlistImporter.connectConstant` counts `BIT_X` occurrences and folds them to 0 (`src/jls/hdl/imp/NetlistImporter.java:758-773`), reporting the loss through a field literally named `coercedX` (`src/jls/hdl/imp/ImportSummary.java:27-28,53-60,93-99`). `YosysNetlist.java:26-27` states the policy: "later coerces `x` to 0 and `z` to a disabled TriState (JLS is two-state); this class just preserves what Yosys said." `hdl-support-research.md:468-469` lists the same as one of five loud-rejection gaps. Related blocked cells: `$adff` family (async reset — `Register` has only `D` and `C` inputs, `src/jls/elem/Register.java:230-231,240-241,250-251,260-261`), `$dlatch` with set/reset, `$tribuf` strength. | **V1** + **V5** |
| **76** | Structural gate-level Verilog netlist export — **HAVE** | Cell libraries emit strength-annotated and UDP-based primitives; JLS's export cannot round-trip through one. Low urgency, same root cause. | **V1** + **V2** |
| **83** | JEDEC **JESD3-C** fuse map (the "burn a GAL22V10" item the survey calls "a genuinely delightful teaching artifact", §6) | GAL/PAL output macrocells are three-state **and** open-collector-capable, with product-term OE. Emitting a fuse map means modelling the OE and the driver kind honestly. | **V3** + **V6** |
| **89** | IEEE **1497 SDF** | Already diagnosed under the old filter: `docs/standards-adoption/11-costed-rejections.md:66` — "C4 — honor `TIMINGCHECK`: setup/hold violation detection, **which requires a value domain that can express 'violated'**"; and `:230` names "the X-state cascade" as failure mode 2. That is not an argument against SDF. It is an argument that SDF is gated on V1+V8, and it says so. | **V1** + **V8** |
| **129** | IEEE **1149.1 JTAG + BSDL** — survey "COULD (teaching value)", Tier 9's only reachable row | BSDL's `BOUNDARY_REGISTER` cell types are `INPUT`/`OUTPUT2`/`OUTPUT3`/`BIDIR`/`CONTROL`/`CONTROLR`, and the pin map declares `linkage`, `bidir`, and open-drain pins. A boundary-scan chain is a drawable circuit only if the tool has bidirectional and three-state pin cells to draw. | **V3** + **V6** |
| **49 / 50** | SVA (in 1800) / IEEE **1850 PSL** | Both are defined over four-state values; `$isunknown`, `$rose`/`$fell` with X-handling, and the entire "assert that this never goes X" idiom are the reason assertions exist in teaching. Survey marks both OTHER; the OTHER mark is partly a value-domain artifact. | **V1** |
| **58** | **cocotb** — survey ADJACENT | cocotb's `BinaryValue`/`LogicArray` is four-state; a JLS↔cocotb co-simulation boundary (the natural shape of Stage 3, `hdl-support-research.md:396-408`) cannot represent an X in either direction. | **V1** + **V6** |
| **64** | AIGER / **BTOR2** | AIGER is two-state (fine). BTOR2 and any SMT back end model *unconstrained* inputs, which is the formal cousin of X; a JLS→BTOR2 path would have to decide what an undriven net means, and today the answer is "0", which is unsound for verification. | **V1** |
| **4** | IEEE **1685 IP-XACT** — survey's only realistic Tier 1 addition | `wire` port direction in IP-XACT is `in`/`out`/`inout`/`phantom`. JLS subcircuit boundaries have `InputPin` and `OutputPin` and nothing else (`src/jls/elem/InputPin.java`, `OutputPin.java`), so a JLS component can never be an IP-XACT component with a bidirectional interface — which is most real bus interfaces. `docs/standards-adoption/08-ipxact-export.md` exists; this is its ceiling. | **V6** |
| **82** | **XDC / QSF / LPF** constraints — **ROADMAP** (#213 follow-ups) | Every vendor constraint language carries per-pin `PULLUP`/`PULLDOWN`/`PULLMODE` and `DRIVE`/`OPENDRAIN`/`IOSTANDARD` attributes. `docs/standards-adoption/06-fpga-constraint-formats.md:325` already sketches a `led[0] LED1 pull=up` grammar and `:556` lists "pull-ups, drive strength… input/output" as the deferred set — deferred because JLS has no model-side concept for any of them to bind to. | **V2** + **V3** |
| **65 / 259** | `riscv-arch-test` / RISCOF — the survey's **#2 ranked** logic-design item | Compliance suites assume architectural state is **undefined at reset** and that the DUT's reset sequence establishes it. JLS establishes it for free: `LogicElement.initInputs` zeroes every input at every depth (`docs/simulation-semantics.md:135-141`) and `Register.initSim` drives the configured `init` (`:157-161`). A JLS CPU therefore passes tests a real CPU would fail, because JLS is quietly supplying a reset the design does not have. This is a *correctness* gap in the tool's flagship conformance ambition. | **V5** |
| **—** | JLS's own `-t` test-vector format (`docs/batch-interface.md` §2), against the Logisim-evolution catch-up bar | Logisim's documented test-vector format supports "don't-cares, high-Z, sequential mode" (`docs/hdl-support-research.md:186-189`), named as "the concrete catch-up bar for JLS's batch mode". JLS's grammar admits only integers (`docs/batch-interface.md:88-91`): no `x`, no `z`, no `-`. Autograders cannot express "this output may be anything" or "this pin is released". | **V1** + **V7** |

Two entries deserve a note because the survey marks them HAVE and this sweep does
not contradict that: **#26** and **#66**. JLS genuinely conforms to the *syntax*
of both. What it does not have is the *semantics* either standard was written to
carry. The distinction matters because `docs/standards-landscape.md:75` cites
`VhdlEmitter.java:470` — the `when others` line — as the **evidence** for the
1164 HAVE mark, and that line is the exact place where the emitter satisfies a
nine-value type's coverage rule from a two-value simulator. The frame's known
example is confirmed, and it is worse than stated: the emitter also hard-codes
`(others => 'Z')` for tri-state (`VhdlEmitter.java:345`) and writes a header
comment disclaiming X (`:100-101`), so there are three separate places where the
generated VHDL asserts a value model the simulator does not implement.

---

### The changes, and what each unlocks

Eight distinct model changes. **V1 is the spine**; V2–V8 are independently
valuable but V2, V4, V6, V7, V8 all sit on top of it. V3 and V5 could technically
ship before V1 but would be crippled without it.

---

#### V1 — A per-bit four-state value type (`0 / 1 / X / Z`) replacing `@Nullable BitSet`

**What it is technically.** Introduce a `LogicVector` value type in the headless
core and make it the currency of `Input.setValue`, `Output.propagate`,
`WireNet.propagate`, `SimEvent.NewValue`, and every `react`. The representation
that costs least and is best-precedented is the **aval/bval two-plane encoding**
IEEE 1364 already uses in its VPI `s_vpi_vecval` struct: two `BitSet`s `a` and
`b`, with `(a,b)` = `(0,0)`→`0`, `(1,0)`→`1`, `(0,1)`→`Z`, `(1,1)`→`X`. Two-state
values keep `b` empty, which makes the common case a single-plane object and
lets bitwise AND/OR/XOR stay two `BitSet` ops instead of a per-bit loop.

Make it a **sealed interface** with a `Binary` case (one plane, no unknowns, no
HiZ — bit-identical to today's `BitSet` path) and a `FourState` case. This is not
gold-plating: it is what keeps the hot plane honest (see *Ripple effects*), and
it fits the sealed/records/exhaustive-dispatch program `docs/grand-architecture.md`
§4.3 and issue #95 already run (`SimEvent.Payload` is the existing precedent,
`src/jls/sim/SimEvent.java:22-24`).

`null` stops meaning HiZ. HiZ becomes a per-bit state, which retires
`docs/simulation-semantics.md:55-56` ("HiZ is all-or-nothing per signal") and
`docs/batch-interface.md:302-304` ("mixed vectors like `b1z0` cannot [occur]").

**Standards unlocked:** #25, #26, #27, #30/#32, #31, #33, #47, #66 (fully), #68,
#72, #75, #76, #49/#50, #58, #64, and the `-t` grammar. It is the precondition
for V2, V4, V6, V7, V8 — i.e. it is on the path of #67, #89, #129, #4, #82, #83,
#43, #44 as well. **Sixteen entries directly, twenty-four counting its
dependents.** Nothing else in the survey has that reach.

**Pedagogical capability unlocked:**
- *Unknown propagation as a teachable phenomenon.* Today a student who forgets to
  drive a net sees `0` and their circuit "works". With V1 they see `x` spread
  through the cone and they learn to trace it back — the single most transferable
  debugging skill in RTL.
- *Don't-know vs don't-care.* Two different X-like things that students conflate
  for years. V1 gives the first a value; V7 gives the second one.
- *Honest gate tables.* `1 AND x = x` but `0 AND x = 0`; `1 OR x = 1`. The
  three-valued gate tables are a classic first-year exercise that JLS currently
  cannot even demonstrate, let alone assign.
- *X-pessimism as a concept.* Why simulators report X where hardware would settle
  — and why that is the safe direction. Impossible to raise without an X.
- *Reading somebody else's waveform.* Students who move to GHDL/Icarus/Vivado hit
  `x` in hour one and have never seen it.

**What JLS papers over today:** 29 explicit `if (value == null) value = new
BitSet()` coercion sites across 17 element classes (`Adder` 3, `AndGate` 1,
`Binder` 1, `Decoder` 1, `DelayGate` 1, `Memory` 8, `Mux` 2, `NandGate` 1,
`NorGate` 1, `NotGate` 1, `OrGate` 1, `Register` 2, `ShiftRegister` 2,
`StateMachine` 1, `TriState` 1, `TruthTable` 1, `XorGate` 1). Each is a place
where "I am not being driven" is silently rewritten to "I am zero" — spelled out
as intended behaviour in `docs/simulation-semantics.md:60-66`. Plus:
`TraceSample`'s HiZ marker (`src/jls/sim/TraceSample.java:6-17`: "The BitSet
width is the element's bit count plus one, with the extra top bit set to mark a
HiZ value") smuggles the fourth state through an out-of-band width hack;
`BitSetUtils.toDisplay` returns the string `"HiZ"` for `null`
(`src/jls/BitSetUtils.java:237-241`); `BatchSimulator` normalises HiZ to a marker
BitSet before comparing samples (`src/jls/sim/BatchSimulator.java:160-166`);
`ImportSummary.coercedX` is a counter whose only job is to report information
destroyed on import. Four independent workarounds for one missing state.

**Honest size: 10–14 maintainer-weeks.** Anchors: 417 `BitSet` references across
51 files in `src/`; 134 across 21 files in `test/`; 24 concrete `react(long,
Simulator, SimEvent.Payload)` implementations (25 files match `public void
react(`, of which `LogicElement.java:533-536` is the base that throws
`UnsupportedOperationException("no react")`); 8 concrete `computeOutput()`
implementations (`AndGate`, `NandGate`, `NorGate`, `NotGate`, `OrGate`,
`XorGate`, `DelayGate`, `Extend`, over the abstract declaration at
`src/jls/elem/Gate.java:663`). Roughly: type + operations + tests 2 weeks;
element pass (24 reacts, 8 computeOutputs, `Output`/`Input`/`WireNet`/`Pin`/
`Display`) 4–5 weeks; trace/VCD/stdout/probe surfaces 1–2 weeks; file format and
loader 1 week; re-deriving and re-approving every golden 2–3 weeks; rewriting
`docs/simulation-semantics.md` §2/§5/§6/§9/§10/§12 and `docs/batch-interface.md`
§2/§3.4/§4.3 1–2 weeks.

---

#### V2 — A drive-strength lattice and a real resolution function on `WireNet`

**What it is technically.** Give each driven bit a strength pair
(`strength0`, `strength1`) over Verilog's 8-level lattice
(`supply=7 … highz=0`), and replace `WireNet.propagate`'s scan-and-take-first
with a **resolution function**: fold all active drivers per bit, strongest wins,
equal-strength disagreement → `X`, all-highz → `Z`. Cache the driver list at
elaboration time (`WireNet.makeNet` / `WireNet.recheck`,
`src/jls/elem/WireNet.java:97-165`, `:272-302`, already the right home — they
already walk ends to compute `bits`, `hasinput`, `triState`) so the per-event
resolution loop runs over an array instead of re-walking `ends` and calling
`instanceof` on every `Put` the way `:457-471` does today.

Simplification available and worth taking: expose only **three** strengths in the
UI (`strong` / `pull` / `highz`), which is all IEEE 1164 needs (`0/1` vs `L/H` vs
`Z`), and keep the full 8 internally for Verilog fidelity.

**Standards unlocked:** #67 EVCD (its recorded revisit trigger, verbatim:
"JLS gains a drive-strength value domain", `07-waveform-formats.md:150-151`),
#31 Verilog strength constructs, #25/#26 resolution functions, #76, #82 (`DRIVE`,
`PULLMODE`), #83 (macrocell OE), and the resolution half of #22 I²C.

**Pedagogical capability unlocked:**
- *Honest bus contention.* Today two enabled drivers with different values
  produce a deterministic winner ("first active driver in net order") plus a
  warning dialog (`WireNet.java:472-483`), and the semantics doc admits the
  resolution "stays deterministic" (`docs/simulation-semantics.md:433-441`).
  That teaches students that bus conflicts have an answer. They do not. With V2
  the net goes `X`, the `X` propagates, the waveform shows it, and the student
  fixes the enable logic instead of ignoring a dialog.
- *Why a pull-up is not a driver.* `pull` loses to `strong` and wins over
  `highz` — the entire point of the strength lattice, and the thing that makes
  open-drain work. Undemonstrable without it.
- *Bus-hold / keeper circuits, weak drivers, and why `wire`≠`wand`.*

**What JLS papers over today:** the `TellUser.warn` bus-conflict dialog
(`WireNet.java:477-483`) is a warning *in place of a value* — the tool knows the
situation is undefined and reports it out-of-band because the value domain cannot
carry it. `07-waveform-formats.md:108-114` confirms the absence is total: "the
`strength` identifier exists [nowhere] under `src/jls/sim/`".

**Honest size: 4–6 maintainer-weeks** on top of V1. The lattice and fold are
small; the cost is the elaboration-time driver cache, `WireNet.makeNet`/`recheck`
rework, the conflict-reporting rewrite (the once-per-run warning becomes an
optional diagnostic on top of a real X), and re-deriving
`SimulationSemanticsRegressionTest.multiDriverConflictResolvesDeterministicallyAndWarnsOnce`,
which currently *pins the wrong behaviour as correct*.

---

#### V3 — Driver kind and net kind: open-drain outputs, wired-AND/OR nets, drawable pull-up/pull-down

**What it is technically.** Three additions, all small once V1+V2 exist:
1. An `Output` gains a **driver kind**: `pushPull` (today's only behaviour),
   `openDrain` (drives 0 strongly, releases to Z for 1), `openSource` (dual).
   `Output.propagate` (`src/jls/elem/Output.java:136-170`) maps the logical value
   through the driver kind before handing it to the net.
2. A `WireNet` gains a **net kind**: `tri` (resolved, today's), `wand`/`wor`
   (wired-AND / wired-OR resolution), `tri0`/`tri1` (resolve to 0/1 when
   undriven). Net kind is a *drawing* property like `triState` already is
   (`WireNet.java:29-30`, `:359-388`) and propagates the same way through
   `TriProp` implementors.
3. A new **Pull element** (`PullUp`/`PullDown`, one palette entry with a
   direction attribute) that drives a constant at `pull` strength. Drawable,
   which is better than a net attribute: nets are reconstructed from wires at
   load (`Circuit.finishLoad`) and have no save representation, whereas an
   element does — and IEEE 315 / IEC 60617 already have the resistor symbol
   (#45).

**Standards unlocked:** #22 I²C (completely — this is the whole standard's
electrical model), #31 (`wand`/`wor`/`tri0`/`tri1`/`pullup`/`pulldown`),
#43 and #44 (the open-collector/open-emitter output qualifying symbols that
`01-iec-ieee-symbols.md:44-46` currently excludes from the conformance claim),
#45, #47 (WaveJSON `u`/`d`), #82 (`PULLUP`/`PULLDOWN`/`OPENDRAIN` pin
attributes — `06-fpga-constraint-formats.md:325,556`), #83, #129.

**Pedagogical capability unlocked:**
- **The open-drain I²C lab.** SDA and SCL as wired-AND nets, every device an
  open-drain driver, one pull-up per line. Start/stop conditions, ACK/NACK (the
  slave *pulls down* during the ACK bit — meaningless without open-drain),
  clock stretching (a slave holds SCL low; the master's release does not raise
  it), and multi-master arbitration, which **is** wired-AND: the master that
  writes 1 and reads 0 has lost. This is a complete, self-contained, genuinely
  industrial lab that JLS cannot host at all today.
- *Why the bus needs a resistor at all* — the first time most students meet a
  passive component in a logic course, and the reason "open-drain" is not just
  "a weird buffer".
- *Interrupt lines and wired-OR.* Shared active-low IRQ from N devices, one
  wire, no arbitration logic. Two gates' worth of drawing, a real design idiom.
- *Level shifting and mixed-voltage buses* as a discussion the model can support.
- Completes the #43/#44 symbol-conformance claim rather than shipping it with a
  documented hole.

**What JLS papers over today:** nothing — and that is the point. There is no
workaround because the concept is simply absent: the only occurrences of
"open-drain", "wired-and", "pull-up" in the tree are the Verilog reserved-word
list (`src/jls/hdl/HdlNames.java:51`), an HTML tutorial's "pull-down list"
(`src/jls/tutorial/tutorial3.html:70`), a constraint-format grammar sketch for a
feature that cannot bind to anything (`06-fpga-constraint-formats.md:325`), and
`docs/simulation-semantics.md:439` stating the absence.

**Honest size: 3–5 maintainer-weeks** on top of V1+V2. New element (`PullUp`,
following the `ElementRegistry`/`ElementType` ritual and the palette/dialog
split), two enum attributes, resolution-function cases, save-format additions,
symbol rendering for the qualifiers, and one worked example circuit per idiom.

---

#### V4 — The IEEE 1164 nine-value mapping layer (`U W L H -`)

**What it is technically.** With V1 (four states) and V2 (strength) in place, the
1164 nine-value type is a **presentation and interchange mapping, not a fifth
mechanism**: `U` = uninitialized (V5's start-up state), `X` = strong unknown,
`0`/`1` = strong forcing, `Z` = high-impedance, `W` = weak unknown, `L`/`H` =
weak 0/1 (strength `pull`), `-` = don't-care (V7). Add the mapping in both
directions, plus `resolved`-table equivalence tests against the standard's own
resolution table.

**Standards unlocked:** #26 for real (turning a syntactic HAVE into a semantic
one, and letting `VhdlEmitter` delete its disclaimer header at
`VhdlEmitter.java:100-101` and its class-doc caveat at `:24-25`), #25 resolution
functions, #27 (`TO_X01`, `is_X`), and honest GHDL differential testing in the
existing `test/jls/hdl/GhdlCompileTest.java` skip-when-absent harness — today
JLS can compile its exported VHDL but cannot *compare simulation results*
meaningfully, because the two simulators do not share a value domain.

**Pedagogical capability unlocked:** the `std_logic` table is taught in every
VHDL course on earth, and JLS becomes the tool that *shows* it rather than the
tool whose export mentions it. Students can see why `'L'` and `'0'` are different
and why a resolution function needs nine rows.

**What JLS papers over today:** the three `VhdlEmitter` sites named under #26
above — `when others` full coverage (`:467-471`, `:575-580`), `(others => 'Z')`
(`:345`), and the generated-header disclaimer (`:100-101`).

**Honest size: 2–3 maintainer-weeks** on top of V1+V2+V5. Mostly tables, a
round-trip test against the 1164 resolution table, and the emitter cleanup.

---

#### V5 — Uninitialized start-up state, and reset discipline

**What it is technically.** Stop forcing everything to 0 at time 0. Today
`LogicElement.initInputs` sets every input at every depth to 0
(`docs/simulation-semantics.md:135-141`) and outputs start at 0 or `null`
(`:152-161`). Change the default start-up value to `U`/`X` for **state-holding**
elements — `Register` (whose `init` attribute becomes *optional*, and defaults to
unknown), `Memory` (whose uninitialized words read `X`, not 0), `StateMachine`
(unknown until reset or a marked initial state) — and let it propagate. Give
`Register` the **async reset/preset pins** it lacks (`src/jls/elem/Register.java`
declares only `D` and `C`, lines 230-231/240-241/250-251/260-261), which is the
other half of teaching reset and is independently the `$adff` gap named in
`hdl-support-research.md:468` and §6 Stage 2.

Ship it behind a per-circuit attribute (`init-model = zero | unknown`) defaulting
to `zero` for one release so no existing lab breaks; flip the default at the next
major version.

**Standards unlocked:** #75 (`$adff`/`$adffe`/`$sdff` families become importable
rather than loudly rejected — the single largest named gap in
`hdl-support-research.md` §7.2), #65/#259 `riscv-arch-test`/RISCOF (a compliance
suite against a CPU that JLS is secretly resetting for free is not a compliance
claim), #26/#4 (`U`), #33.

**Pedagogical capability unlocked:**
- **Reset discipline, at all.** Today every register powers up at its configured
  `init` value, so a design with no reset network simulates perfectly and the
  student never learns why reset exists. With V5, an unreset design shows `X` on
  its state outputs forever, and the fix — draw a reset tree, hold it for N
  cycles, release it synchronously — is a lab with a visible pass/fail.
- *Sync vs async reset*, and why one of them needs a reset-release
  synchronizer.
- *Uninitialized memory.* A program that reads before writing gets `X`, not 0.
  In the `riscv/` CPU trajectory this is the difference between a lab that finds
  the bug and a lab that hides it.
- *Why FPGAs and ASICs differ* on power-up state — a genuinely useful thing to
  know before a student's first bitstream.

**What JLS papers over today:** `Register.initSim` posts a time-0 event driving
the `init` attribute (`docs/simulation-semantics.md:157-161`), pinned by
`SequentialGoldenTest.registerInitialValueAppearsBeforeAnyClockEdge` — a golden
that *asserts* the fiction. `Memory.initSim` copies `initMem` into `mem` and only
then sets the output null (`src/jls/elem/Memory.java:1309-1315`). `initInputs`
zeroing is pinned by
`SimulationSemanticsRegressionTest.initInputsReachesInsideSubcircuits`.

**Honest size: 2–4 maintainer-weeks** on top of V1 (the reset/preset pins are
~1 of those, and they touch `Register`'s geometry, dialog, save attributes, and
the four orientation branches).

---

#### V6 — Bidirectional ports: an `inout` pin element and a bidirectional subcircuit boundary

**What it is technically.** A new `BidirPin` element (or a direction attribute on
`Pin`) whose `Put` is simultaneously a driver and a reader on the same net, with
the resolution of V2 deciding what it reads back. Subcircuit boundaries gain
`inout` ports, which means `SubCircuit.initSim`/`react` and the
`InputPin`/`OutputPin` boundary machinery (`src/jls/elem/InputPin.java:160-200`
already special-cases the tri-state-net case at the boundary) gain a third case.

**Standards unlocked:** #67 EVCD (its *other* recorded revisit trigger, verbatim:
"or a bidirectional pin element", `07-waveform-formats.md:150-151`; EVCD's whole
payload is `$var port` records with direction), #4 IP-XACT (`inout` ports —
without which a JLS component cannot describe any real bus interface), #129
BSDL (`BIDIR` boundary cells), #43/#44 (the signal-flow-direction reversal arrows
excluded at `01-iec-ieee-symbols.md:44-46`), #22 I²C at a subcircuit boundary
(SDA must cross the boundary in both directions), #58 cocotb, #82 (bidirectional
IO buffers).

**Pedagogical capability unlocked:** a memory module with a shared data bus drawn
as a *subcircuit* rather than as two separate in/out ports — which is how every
real SRAM, every microcontroller GPIO, and every I²C peripheral is packaged.
Today a student building a bus-based system must break the abstraction at every
module boundary. Also: GPIO direction control as a design exercise (drive vs
release vs read-back), and the read-back-verify idiom that open-drain buses
depend on.

**What JLS papers over today:** `InputPin.initSim`'s special case — an imported
input pin attached to a tri-state net sets its value to `null`
(`src/jls/elem/InputPin.java:168-181`) — is a half-measure toward bidirectionality
at the boundary. `WireNet.setTriState`'s propagation through `TriProp` pins
(`WireNet.java:359-388`) is the same seam.

**Honest size: 4–6 maintainer-weeks** on top of V1+V2. The element is small; the
cost is the subcircuit boundary (`SubCircuit`, `InputPin`, `OutputPin`,
`Circuit.finishLoad`, the HDL exporter's port walk in `HdlExporter.buildModel`,
the `.jls` format, and the `-pins`/board binding path which today assumes
unidirectional ports).

---

#### V7 — Unknown-aware element semantics and first-class don't-cares

**What it is technically.** The half of V1 that is not the type: decide, per
element, what each of the 24 `react` methods does with `X`/`Z` inputs, and
document it normatively. Gates get proper three-valued tables (`0 AND x = 0`,
`1 AND x = x`). `Mux` with an unknown selector outputs `x` unless all candidate
inputs agree (the standard X-optimism/pessimism choice, which should be a
documented, switchable policy). `Register` with an unknown clock does not latch
and drives `x`. `Memory` with an unknown address reads `x` and, on write,
corrupts nothing (or optionally everything — a real pessimism choice).
`Decoder`/`Adder`/`ShiftRegister` propagate.

Separately: promote **don't-care** from an editor-only concept to a value.
`TruthTable` already stores don't-cares as `2` (`src/jls/elem/TruthTable.java:79`)
and the editor supports entering them, but `react` destroys output don't-cares —
`"don't care becomes false"` (`:1447-1449`) — and the HDL model records the loss
in a comment (`src/jls/hdl/HdlModel.java:617`). Add `-` to the `-t` test-vector
grammar as "this output may be anything" (matching the Logisim catch-up bar).

**Standards unlocked:** #27 (`std_match`, `TO_X01`), #30/#32 (synthesis
don't-cares actually reaching the synthesizer), #25/#26, #49/#50, the `-t`
grammar, and the correctness half of #75.

**Pedagogical capability unlocked:** the difference between "I do not know" and
"I do not care" — the single most common conceptual confusion in a first
digital-logic course, and one JLS currently *causes* by collapsing both to 0.
Karnaugh-map don't-cares become end-to-end real: mark `-` in the truth table,
watch the synthesizer exploit it, see the smaller gate count. Autograders gain
"output unspecified here" instead of over-constraining student designs.

**What JLS papers over today:** `TruthTable.react:1447-1449` and
`HdlModel.java:617` ("don't care **already lowered**") are the same fact stated
twice. `Mux.react` treats an out-of-range selector as producing a fresh zero
(`src/jls/elem/Mux.java:529-535`). `Splitter.react` propagates HiZ to *all*
outputs if the input is HiZ (`src/jls/elem/Splitter.java:208-215`) because it
cannot split a partially-undriven vector; `Binder` does the mirror-image thing
(`src/jls/elem/Binder.java:245` and `docs/simulation-semantics.md:447-452`) —
both disappear once HiZ is per-bit.

**Honest size: 3–5 maintainer-weeks** of *incremental* work beyond V1's element
pass (V1 must touch all 24 reacts anyway; V7 is the difference between "compiles
and preserves today's behaviour" and "is semantically right"), plus the normative
§6/§10 rewrite and a new golden family.

---

#### V8 — Timing-checked unknowns: setup/hold violations and metastability

**What it is technically.** Give `Register`, `Memory` (sync-write mode), and
`StateMachine` optional `setup`/`hold` attributes. On a violation, drive `X` for
one output period instead of a defined value. Optionally, a `Metastable` mode
that resolves the `X` after a configurable settling time. This is the smallest
possible timing-check facility and deliberately **not** a timing engine — it adds
a check at an existing edge-detection site (`Register.react`'s remembered-clock
scheme, `docs/simulation-semantics.md:299-311`), not a new delay model.

**Standards unlocked:** #89 SDF's `TIMINGCHECK` section — the item
`11-costed-rejections.md:66` correctly identified as "requir[ing] a value domain
that can express 'violated'" and `:230` called "the X-state cascade". With V1 the
cascade is already paid for; the check itself is cheap. Also #49/#50 (assertions
about metastability), #31 (`$setup`/`$hold`/`$setuphold` specify blocks).

**Pedagogical capability unlocked:** **clock-domain crossing and synchronizers.**
Today a two-flop synchronizer in JLS is two flip-flops that do nothing, because
JLS has no setup/hold and no metastability, so the student cannot see the problem
the circuit solves. With V8, an asynchronous input violating setup drives `X` into
the first flop; the second flop resolves it; and the lab has a visible before/
after. This is the most important thing in a sequential-logic course that JLS
currently cannot teach at all. Also: why FIFOs need gray-code pointers, why
`async_reset` needs a synchronized release, and what a timing report is *for*.

**What JLS papers over today:** `docs/simulation-semantics.md:200-204` states it
plainly — "Same-time races (e.g. a clock edge and a data change at the identical
timestamp) are resolved by this read-latest rule plus FIFO event order —
deterministically, but **with no setup/hold modeling**." The determinism is a
feature; the silence about the violation is the gap.

**Honest size: 3–4 maintainer-weeks** on top of V1. Two attributes, a check at
three edge-detection sites, a settling-time event, and goldens.

---

### Ripple effects

**Normative documents.** `docs/simulation-semantics.md` is *the* blast radius.
V1 rewrites §2 entirely and materially edits §5 (initialization), §6.1
(propagation and the null pass-through), §6.2 (the `toBeValue` change check
becomes a four-state compare), §9 (tri-state and multi-driver resolution —
replaced wholesale by a resolution function), §10 (splitter/binder HiZ rules
vanish), and §12 (the golden mapping table). §7's delay table is untouched. The
appendix's S1 verdict ("multi-driver conflicts resolved by hash order — *bug,
fixed*") gets a successor entry: the #98 fix made the wrong answer deterministic;
V2 makes it the right answer.

`docs/batch-interface.md` is a **stability contract** ("any change to them
requires a CHANGELOG entry and either a major version bump or a compatibility
flag that preserves the old behavior", `:6-11`). V1/V7 change §2 (test-vector
grammar gains `x`/`z`/`-`), §3.4 (value display gains X/U/W/L/H), and §4.3 (VCD
gains `x` and mixed vectors like `b1z0`). The honest route is a
`--value-model=two-state|four-state` flag defaulting to `two-state` for one
release, then a major bump — the same shape `Memory`'s `sync` attribute used
(`docs/simulation-semantics.md:406-408`: "The default is unchanged for every
pre-#199 circuit").

`ARCHITECTURE.md:359-368` carries a **binding equivalence criterion** for any
future simulation strategy: it must be "observably identical to the event model
as specified in `docs/simulation-semantics.md` — the two-states-plus-HiZ value
domain and multi-driver/tri-state resolution (§2, §9)…". That clause has to be
re-anchored to the new §2/§9 before any of this lands, and it says exactly how:
"Any divergence is a specified, documented change to `docs/simulation-semantics.md`
first, never a silent behavioral difference." The process for this change is
already written down.

Also touched: `docs/standards-landscape.md` §5 and §13.1 item 4 (EVCD's
"strength/direction half… is now known to be unreachable" —
`07-waveform-formats.md:156-159` — becomes reachable and must be un-said);
`docs/standards-adoption/07-waveform-formats.md` (its "do NOT do EVCD, ever" at
`:572` was scoped to the current semantics and its own revisit triggers at
`:150-151` fire); `docs/standards-adoption/11-costed-rejections.md` §1 (SDF's C4
and failure-mode-2 arguments are retired); `docs/standards-adoption/01-iec-ieee-symbols.md:44-46`
(the excluded output qualifiers come back into the claim);
`docs/hdl-support-research.md` §6 Stage 2 and §7.2 (the x/z and `$adff` rejection
lists shrink); `docs/file-format.md` §4 and §6.

**The file format.** `.jls` is version-negotiated: `FORMAT 1`/`FORMAT 2` today,
with the rule that "a writer MUST emit the header with the highest version whose
features the file uses" (`docs/file-format.md:191-195`) and readers MUST accept
any declared version ≤ their own (`:180-183`). So this is a **`FORMAT 3`** with
no compatibility problem for existing circuits: a file that uses no new feature
still writes `FORMAT 2` and still loads in old JLS. New payloads: `Output` driver
kind, net kind (carried on the elements that determine it, as `triState` already
is), the `PullUp`/`PullDown` element tag (the tag table at
`docs/file-format.md:291` grows from 32 to 33+), `Register` reset/preset pin
presence and `init` becoming optionally `unknown`, `Memory` init encodings gaining
`x`/`u` digits (see `test/jls/elem/MemoryInitEncodingTest.java`), `BidirPin`, and
a per-circuit `init-model` attribute. `AllElementsRoundTripTest`,
`SaveTagsTest`, `FileFormatSpecTest`, `FormatHeaderTest`, `DeterministicSaveTest`,
and `GenerativeRoundTripFuzzTest` all extend rather than break.

**Element `react()` methods — the count.** **24 concrete implementations** must be
visited (`Adder`, `Binder`, `Clock`, `Constant`, `Decoder`, `Display`, `Extend`,
`Gate`, `InputPin`, `JumpEnd`, `JumpStart`, `Memory`, `Mux`, `OutputPin`,
`Pause`, `Register`, `ShiftRegister`, `SigSim`, `Splitter`, `StateMachine`,
`Stop`, `SubCircuit`, `TriState`, `TruthTable` — 25 files match `public void
react(` including `LogicElement.java:533-536`, the base that throws). Plus **8
`computeOutput()` implementations** behind `Gate.react` (`AndGate`, `NandGate`,
`NorGate`, `NotGate`, `OrGate`, `XorGate`, `DelayGate`, `Extend`). Plus the 29
null-to-zero coercion sites enumerated under V1, which are the specific lines
that change meaning. Of the 24, roughly 8 are pure pass-throughs where V1 is
mechanical (`Constant`, `JumpEnd`, `JumpStart`, `InputPin`, `OutputPin`, `Stop`,
`Pause`, `SigSim`), 6 are structural and get *simpler* under per-bit HiZ
(`Splitter`, `Binder`, `Extend`, `SubCircuit`, `Clock`, `Display`), and 10 need
real semantic decisions (`Gate`+the 8 computeOutputs, `Adder`, `Decoder`, `Mux`,
`Register`, `ShiftRegister`, `StateMachine`, `TruthTable`, `TriState`, `Memory`).
`Memory` alone has 8 of the 29 coercion sites.

**The simulation hot loop.** `docs/grand-architecture.md` §6 protects it
explicitly: "The discrete-event loop runs entirely within the `core` module…
with zero plugin indirection, no capability lookup, no cross-module call per
event." V1 does not violate that rule — it stays entirely inside `core` and the
loop itself (`Simulator.runEventLoop`, `src/jls/sim/Simulator.java:215-243`) is
untouched. The cost is in what an event *carries*: today `SimEvent.NewValue`
holds one `BitSet` and per-event work is one clone plus one `equals`
(`Output.propagate:139-153`, `WireNet.propagate:496-516`). Two-plane values
double both in the worst case. Three mitigations, all core-internal and therefore
allowed by §6:
1. The **sealed `Binary`/`FourState` split**: a value with no unknown and no HiZ
   bits stays single-plane, so gate circuits that never go X pay approximately
   nothing. This is a value-level version of the same specialize-once idea
   ARCHITECTURE.md's `#221` revisit note reserves for a levelized pass.
2. **Elaboration-time driver caching in `WireNet`** (V2). Today the tri-state
   path re-walks `ends`, does `end.isAttached()`, `e.getPut()`, and an
   `instanceof Output` on *every* propagate (`WireNet.java:457-471`). Caching
   the driver array in `makeNet`/`recheck` makes strength resolution **faster
   than today's first-driver scan**, not slower.
3. **Immutable value objects** so the defensive `clone()` at
   `WireNet.java:496-498`, `:513-516`, `Output.java:148-152`,
   `TriState.java:501-504` (and ~15 more sites) can be deleted outright. Net
   allocation goes *down*.

The oracle for all of this already exists: `test/jls/RiscvCpuGoldenTest.java`
plus `riscv/verify.py`'s differential fuzzing against a reference emulator. Run
V1 in `two-state` compatibility mode and require bit-for-bit identity with the
pre-change RV32I golden — the same criterion `ARCHITECTURE.md:359-368` binds a
future levelized pass to. That is the correct gate, and it is cheap.

**The GUI.** Value rendering: `BitSetUtils.toDisplay` (`src/jls/BitSetUtils.java:237-245`)
gains X/U/W/L/H/mixed forms; `Pin.printValue`, `Register.printValue`, watched-value
overlays, the `Display` element, the signal-trace window (`src/jls/sim/TraceGeometry.java`,
`TraceSample.java` — whose HiZ marker-bit hack is deleted), and the probe path
(`WireNet.java:518-527`, `Wire.getProbe`). New dialogs/attributes: driver kind on
tri-state and output elements, net kind, `PullUp` creation, `Register`
reset/preset, `init-model`. New palette entries need `ElementRegistry`/
`ElementType` entries and the two-layer core/GUI descriptor split
(`docs/grand-architecture.md` §5 notes). Wire rendering should distinguish X
(the conventional red) and Z (the conventional dashed/mid-level) — today the
trace draws HiZ as a mid-level line (`docs/batch-interface.md` §3.4 / §4.3).

**Existing saved circuits.** Unaffected by construction: `FORMAT 3` writes only
when new features are used, `init-model` defaults to `zero`, driver kind defaults
to `pushPull`, net kind defaults to `tri`. The one behavioural change a
pre-existing circuit *would* see is V2's bus-conflict resolution — a circuit that
today silently resolves a conflict to "first driver in net order" will show `X`.
That is the intended correction and should be a CHANGELOG headline, not a
compatibility flag.

**Existing tests.** Directly contradicted (i.e. they pin the current model as
correct and must be re-derived, not merely updated):
- `VcdExportGoldenTest.vcdIsStructurallyWellFormedAndTwoStatePlusHiZ` — asserts
  the absence of `x`. Becomes a mode-conditional assertion.
- `VcdExportGoldenTest.testVectorStimulusVcdMatchesGoldenAndCoversHiZ` — the
  `bz` encoding.
- `SimulationSemanticsRegressionTest.multiDriverConflictResolvesDeterministicallyAndWarnsOnce`
  — pins the wrong answer (V2).
- `SimulationSemanticsRegressionTest.initInputsReachesInsideSubcircuits` — pins
  zero-init (V5).
- `SequentialGoldenTest.registerInitialValueAppearsBeforeAnyClockEdge` — pins
  the free reset (V5).
- `SimulationSemanticsRegressionTest.triStateDoesNotRepostUnchangedOutputEvents`
  — the `toBeValue`/`null` protocol is replaced (V1).
- `SimulationSemanticsRegressionTest.constantValueIsMaskedToTheNetWidth`,
  `memoryDefaultWriteIsLevelSensitiveOnEveryAddressTransient`,
  `stateMachineWithNoMatchingTransitionStaysAliveAndWarnsOnce`,
  `pausePausesOnlyOnNonZeroInput` — survive with signature changes.

Mechanically affected: 134 `BitSet` references across 21 test files, plus
`BitSetUtilsCreateTest`, `BitSetUtilsSumCarryTest`, `BatchSimulationGoldenTest`
(all truth-table goldens), `SequentialGoldenTest` (all five),
`ElementSimulationGoldenTest`, `ShiftRegisterTest`, `MemoryModelTest`,
`RegisterModelTest`, `TruthTableModelTest`, `WireValueChannelTest`,
`VcdProbeExportTest`, `BatchTracePrinterTest`, `RiscvCpuGoldenTest`,
`test/jls/edit/TriStateBundleConnectTest`. `HeadlessCoreRatchetTest` and
`ArchitectureRulesTest` are unaffected — the new type lives in `core` and imports
no AWT.

**Cross-sweep dependency worth flagging:** V1 lands *inside* `jls.core`, which
`docs/grand-architecture.md` §3 names as the not-yet-extracted keystone (#77).
Doing V1 before the core extraction means doing the biggest type migration in the
program across an un-drawn boundary. **Sequence V1 after #77**, or accept that
V1's element pass doubles as part of the extraction.

---

### What genuinely stays out, and why

Only three items in this sweep's scope fail the frame's legitimate tests.

- **#70 FSDB, #71 WLF** (Tier 4) — proprietary binary waveform databases with no
  published format and no independent reader. Not "too big": *unimplementable*
  without a vendor NDA, which makes them a different tool class's problem. VCD
  and FST reach every tool that matters.
- **#69 LXT / LXT2 / VZT** (Tier 4) — GTKWave's own superseded formats, with FST
  as the maintained successor from the same author. Genuinely obsolete with a
  successor.
- **#73 UCDB / vendor coverage databases** (Tier 4) — coverage persistence for a
  constrained-random verification methodology JLS does not have and (per the
  survey's Tier 3 stance, which this sweep does not contest) should not build.
  Different tool class. Note that #53 UCIS, its standardized cousin, is the same
  answer.

Everything else in #25–#47 and #66–#73 marked OTHER by the survey is OTHER for
reasons *outside* this sweep's remit (mixed-signal: #28, #35, #37, #42; C++/HLS:
#34, #36; compiler IR: #39, #40, #41; PLC languages: #46) and is left to the
sweeps that own those tiers. **Nothing in this sweep is declined for being
large.** #67 EVCD in particular is explicitly *un*-declined: the recorded
"do NOT do EVCD, ever" was correctly conditioned on "under the current simulation
semantics", and V2+V6 change the condition.

---

### Sources

**Repository (read at HEAD, this sweep):**

- `docs/simulation-semantics.md` — §2 value domain `:42-66`; §3 events/payloads
  `:68-100`; §5 initialization `:129-171`; §6.1 propagation `:176-204`; §6.2
  delay discipline `:208-241`; §9 tri-state and multi-driver `:409-445`; §10
  bundles `:447-458`; §12 golden mapping `:474-491`; appendix S1/S6/S7
  `:493-526`.
- `docs/batch-interface.md` — §2 test-vector grammar `:74-126`; §3.4 value
  display `:197-201`; §4.3 value section and the "no per-bit HiZ" statement
  `:274-304`; stability-contract preamble `:1-15`.
- `src/jls/elem/WireNet.java` — `triState` field `:29-30`; `makeNet` `:97-165`;
  `recheck` `:272-302`; `setTriState` `:359-388`; net value field `:404-407`;
  `propagate` `:443-529` (tri-state resolution `:454-485`, driver scan
  `:457-471`, conflict warning `:472-483`, input delivery + clone `:487-516`,
  probe feed `:518-527`).
- `src/jls/elem/TriState.java` — `toBeValue` `:442-445`; `initSim` null output
  `:453-461`; `react` `:472-529`.
- `src/jls/elem/Output.java` — `propagate` change-detection and clone
  `:136-170`.
- `src/jls/elem/Memory.java` — `initSim` null output `:1309-1315`; `react`
  `:1334-1460` (8 null-to-zero sites at `:1341-1382`; invalid-address HiZ
  `:1441-1447`).
- `src/jls/elem/Splitter.java` `:204-215`; `src/jls/elem/Binder.java` `:245`;
  `src/jls/elem/Mux.java` `:524-545`; `src/jls/elem/Gate.java` `:657-663`,
  `:680-702`; `src/jls/elem/LogicElement.java` `:529-536`.
- `src/jls/elem/TruthTable.java` — don't-care storage `:79`; `"don't care
  becomes false"` `:1445-1449`.
- `src/jls/elem/Register.java` — input declarations (D and C only, no reset)
  `:230-231`, `:240-241`, `:250-251`, `:260-261`.
- `src/jls/elem/InputPin.java` — tri-state boundary special case `:160-190`.
- `src/jls/BitSetUtils.java` — `Create` `:35-49`; `toDisplay`'s `"HiZ"`
  `:237-245`.
- `src/jls/sim/SimEvent.java` — sealed `Payload` `:14-80`;
  `src/jls/sim/Simulator.java` — `runEventLoop` `:215-243`;
  `src/jls/sim/TraceSample.java` — HiZ marker-bit encoding `:1-20`;
  `src/jls/sim/BatchSimulator.java` — HiZ normalisation `:160-166`, VCD value
  encoding `:522-551`.
- `src/jls/hdl/VhdlEmitter.java` — class-doc two-state caveat `:24-25`;
  `std_logic_1164` use clause `:67`; generated-header disclaimer `:100-101`;
  type mapping `:172-179`; `(others => 'Z')` `:334-345`; **`when others` over
  std_logic's nine values `:467-471`** (the survey's cited evidence for the
  #26 HAVE mark); further `when others` arms `:575-580`, `:658`, `:690`;
  don't-care rendering `:549`.
- `src/jls/hdl/VerilogEmitter.java` `:420`; `src/jls/hdl/HdlModel.java`
  `:527`, `:615-617`; `src/jls/hdl/HdlNames.java` `:51` (`pullup`/`pulldown`
  reserved).
- `src/jls/hdl/yosys/YosysNetlist.java` — x/z policy comment `:21-27`;
  `BIT_X`/`BIT_Z` `:42-45`; bit parsing `:849-906`.
- `src/jls/hdl/imp/NetlistImporter.java` — `connectConstant` x-coercion
  `:753-773`; `src/jls/hdl/imp/ImportSummary.java` — `coercedX` `:27-28`,
  `:53-60`, `:93-99`.
- `docs/standards-landscape.md` — §1 HAVE table `:72-98` (esp. the #26 row at
  `:75`); Tier 2 `:159-196`; Tier 4 `:236-251`; §13.1 `:722-742`; §13.3
  `:771-777`.
- `docs/hdl-support-research.md` — §6 Stage 2 gaps `:348-394`; §7.2 resolution
  `:453-475` (x/z and `$adff` at `:466-470`); Logisim test-vector bar
  `:185-190`.
- `docs/grand-architecture.md` — §3 keystone `:68-98`; §6 two planes and the
  hot-loop rule `:314-342`; §9 exclusions `:419-442`.
- `ARCHITECTURE.md` — the binding equivalence criterion naming
  "two-states-plus-HiZ" `:350-368`.
- `docs/file-format.md` — FORMAT header and version negotiation `:159-195`; tag
  table `:291+`.
- `docs/standards-adoption/07-waveform-formats.md` — EVCD analysis `:59-76`,
  "JLS has no strength model" `:98-134`, **revisit trigger `:150-151`**,
  §13.1 amendment note `:156-159`, "do NOT do EVCD, ever, under the current
  simulation semantics" `:572`, unverified marks `:662-667`.
- `docs/standards-adoption/11-costed-rejections.md` — SDF `TIMINGCHECK`/"value
  domain that can express 'violated'" `:66`; two-state statement `:80-84`; the
  X-state cascade `:225-235`.
- `docs/standards-adoption/01-iec-ieee-symbols.md` — excluded output qualifiers
  `:44-46`.
- `docs/standards-adoption/06-fpga-constraint-formats.md` — `pull=up` grammar
  sketch `:325`; deferred pin attributes `:556`.
- Tests referenced: `test/jls/VcdExportGoldenTest.java`,
  `test/jls/SequentialGoldenTest.java`,
  `test/jls/BatchSimulationGoldenTest.java`,
  `test/jls/SimulationSemanticsRegressionTest.java`,
  `test/jls/RiscvCpuGoldenTest.java`, `test/jls/ElementSimulationGoldenTest.java`,
  `test/jls/ShiftRegisterTest.java`, `test/jls/BitSetUtilsCreateTest.java`,
  `test/jls/BitSetUtilsSumCarryTest.java`, `test/jls/VcdProbeExportTest.java`,
  `test/jls/BatchTracePrinterTest.java`, `test/jls/AllElementsRoundTripTest.java`,
  `test/jls/DeterministicSaveTest.java`, `test/jls/FileFormatSpecTest.java`,
  `test/jls/FormatHeaderTest.java`, `test/jls/GenerativeRoundTripFuzzTest.java`,
  `test/jls/HeadlessCoreRatchetTest.java`, `test/jls/elem/MemoryModelTest.java`,
  `test/jls/elem/RegisterModelTest.java`, `test/jls/elem/TruthTableModelTest.java`,
  `test/jls/elem/WireValueChannelTest.java`,
  `test/jls/elem/MemoryInitEncodingTest.java`,
  `test/jls/elem/SaveTagsTest.java`, `test/jls/edit/TriStateBundleConnectTest.java`,
  `test/jls/hdl/GhdlCompileTest.java`, `test/jls/hdl/IverilogCompileTest.java`.

**Mechanical counts (reproducible):**
- `grep -rln "public void react(" src/ | wc -l` → 25 (24 concrete + the throwing
  base in `LogicElement.java`).
- `grep -rln "BitSet computeOutput" src/jls/elem/ | wc -l` → 9 (8 concrete + the
  abstract declaration in `Gate.java`).
- Null-to-zero coercion sites (`== null` immediately followed by `new BitSet()`)
  → **29 sites across 17 element classes**; per-class breakdown in V1.
- `grep -rn "BitSet" src/ --include=*.java | wc -l` → **417** across **51**
  files; in `test/` → **134** across **21** files.
- `src/jls/elem/*.java` → 72 files.

**External documents (not fetched in this pass — treat scope descriptions as
orientation, verify before relying on any clause):**
- IEEE 1364-2001 §18 (VCD) and IEEE 1364-2005 (four-state logic, the 8-level
  strength lattice, `wand`/`wor`/`tri0`/`tri1`/`trireg`, `pullup`/`pulldown`,
  `bufif`/`notif`, `specify`/`$setup`/`$hold`, and the VPI `s_vpi_vecval`
  aval/bval encoding cited as V1's representation precedent). **Unverified.**
- IEEE 1364 Extended VCD (`$dumpports`): the port-state character table and
  whether the two strength fields are the 0–7 Verilog levels — flagged as
  unverified by `07-waveform-formats.md:662-667` and *still* unverified here.
- IEEE 1164-1993/2019 `std_logic_1164`: the nine values `U X 0 1 Z W L H -`, the
  resolution table, and `TO_01`/`TO_X01`/`is_X`. **Unverified.**
- IEEE 1076-2019 (resolved subtypes, resolution functions); IEEE 1076.3
  (`std_match`). **Unverified.**
- IEEE 91-1984 / 91a-1991 and IEC 60617-12: the output qualifying symbols for
  open-collector, open-emitter and three-state, and the bidirectional
  signal-flow arrow. **Unverified** — and the exact clause numbers are exactly
  what `docs/standards-adoption/01-iec-ieee-symbols.md` says a conformance claim
  must cite.
- NXP UM10204 I²C-bus specification (open-drain, wired-AND arbitration, clock
  stretching, pull-up sizing). **Unverified.**
- IEEE 1149.1 / BSDL `BOUNDARY_REGISTER` cell types (`BIDIR`, `OUTPUT3`,
  `CONTROL`). **Unverified.**
- IEEE 1497 SDF `TIMINGCHECK`; Synopsys SAIF `T0/T1/TX/TZ` state-time records;
  WaveDrom/WaveJSON wave alphabet (`0 1 x z = u d 2..9`); GTKWave FST value
  payloads. **All unverified.**
