# 04 — THE INTEGRATED MECHANISM SET

*Integration of six independent mechanism designs (`mech-nested-subcircuits.md`,
`mech-fidelity-toggle.md`, `mech-compiled-backends.md`,
`mech-element-definition.md`, `mech-format-successor.md`, `mech-adversary.md`)
against `BRIEF.md`, `03-determination.md`, and HEAD `803d716`. Every HEAD claim
below was re-verified in this session; where I re-verified a peer's claim I say
so, where I am carrying a peer's measurement I cite the harness.*

---

## 0. Four corrections that reshape the answer, before any verdict

### 0.1 The six designs were written without `docs/capability-roadmap/` — and it already specifies three of them

**Verified at HEAD.** `docs/capability-roadmap/` contains **19 documents**,
including `lf-01-parameterization.md`, `lf-02-compiled-evaluation.md`,
`lf-07-api-and-platform.md`, `lf-06-diff-merge-vcs.md`, and
`keystone-c-performance.md`. Not one of the six mechanism designs cites it.

The overlap is not thematic, it is specific:

| Mechanism design | Recorded program it re-derives |
|---|---|
| `mech-nested-subcircuits.md` (definition/instance split, elaboration) | `lf-01-parameterization.md` — which already states "JLS's hierarchy is elaborated-by-copy already … elaboration is not a new concept to introduce, it is an existing implicit phase that has never had inputs" |
| `mech-compiled-backends.md` (`ExecutionStrategy`, levelized pass) | `lf-02-compiled-evaluation.md` §2.1–§2.7 — engine, two modes, refusal policy, and §2.7 verbatim: *"keeping the two engines identical: specify this, do not hope"* |
| `mech-element-definition.md` (`jls.build.CircuitBuilder`) | `lf-07-api-and-platform.md` §(a) `jls.api` — which already names `riscv/jlsbuild.py` as the workaround that proves the gap |

`03-determination.md` §0.1 already ruled on this and the ruling is carried
forward here: **where a mechanism design duplicates a recorded roadmap program,
the roadmap program wins and the mechanism design contributes its measurements
and its tests.** The practical consequence is that this set builds `jls.api`,
not a fifth independently-invented `CircuitBuilder`/`CircuitForge`/
`CircuitElaborator`/`jls.mach.dsl`.

### 0.2 The format's existing totality invariant is broken at HEAD, and CI is green — verified myself

The adversary found this; I re-verified every leg of it:

| Fact | Anchor (verified this session) |
|---|---|
| `LogicElement` permits 24 types, **including `RegisterFile` and `FieldExtend`** | `src/jls/elem/LogicElement.java:17-22` |
| `ElementRegistry` has **35** `new ElementType(...)` rows, incl. both | `src/jls/elem/ElementRegistry.java:48-49, 62-63` |
| `RegisterFile.save` writes `ELEMENT RegisterFile` | `src/jls/elem/RegisterFile.java:321` |
| `FieldExtend.save` writes `ELEMENT FieldExtend` | `src/jls/elem/FieldExtend.java:291` |
| `SaveTags.WRITABLE` lists **32** tags, neither of these | `src/jls/elem/SaveTags.java:41-71` |
| `docs/file-format.md:291`: "Version-1 and version-2 writers emit exactly these 32 tags" — neither listed | `docs/file-format.md:291` |
| `FORMAT_VERSION` is still 2 | `src/jls/Circuit.java:102` |

Why the conformance test does not catch it, from the test source:
`documentedTagsAreExactlyTheTagsAFullCircuitSaves` compares **doc ↔ a hand-built
fixture** (`test/jls/FileFormatSpecTest.java:294-302`);
`specTagTableAndCodeTagTableAgree` compares **doc ↔ `SaveTags`** (`:337-341`).
The loader routes through `ElementRegistry.forTag` (`src/jls/Circuit.java:918-919`)
— **a third table no test cross-checks against the other two.** Two of three
tables have drifted and the pairwise cycle excludes the one the reader uses.

This is the only *direct measurement* anyone in this study has of format-
maintenance capacity at bus factor 1, and every "forever cost" argument below is
calibrated against it rather than against optimism. It is also decisive on the
`.jlsx` question: opening a second format on top of an unenforced first one is
the worst available move.

**Correction to the adversary's remedy:** it proposed possibly gating the two
tags behind the registry until documented. Do not. `03-determination.md` §0.2 is
right that `RegisterFile` is a first-class multi-read-port element that was
deliberately shipped. The fix is two `SaveTags` entries, two spec rows, and the
missing assertion.

### 0.3 `BRIEF.md` §7 is wrong about the golden oracle, and this changes the `riscv/` deletion order

BRIEF §7 records: *"The golden oracle is 34 simulated cycles and 4 assertions,
**gitignored, never run by CI**, RV32I-only."*

**`git ls-files` at HEAD returns both `test/jls/RiscvCpuGoldenTest.java` and
`test/fixtures/riscv-sum1to10.jls`.** They are tracked. Their *regeneration
path* is inside `riscv/`, which is being deleted. Likewise
`riscv/bench_kernel.py` + `riscv/build/k2000.jls` are the anchor for every
number in `keystone-c-performance.md`, and `riscv/riscv_ref.py` (a 975-line RV32I
reference emulator) + `riscv/fuzz_diff.py` + `riscv/verify.py` are a working
differential harness — the design D5 says *may* survive.

**Consequence for the spine: re-homing those assets is a precondition of the
deletion, not a follow-up.** Three mechanism designs quietly assume fixtures
that `riscv/jlsbuild.py` produced (`mech-nested-subcircuits.md` §12.4 flags this
for `rep*.jls` and is the only one that does).

### 0.4 NEW: 82.3% of all events model no time at all — this reshapes the compiled-tier answer

`keystone-c-performance.md`'s census on the 6,004-cycle RV32I workload, quoted
at `lf-02-compiled-evaluation.md:79-88`: **`PinChanged` is 1,919,891 of
2,331,793 events (82.3%)**. `PinChanged` is the *same-timestamp* notification
`WireNet.propagate` sends every sink, and the zero-delay elements of
`docs/simulation-semantics.md` §6.2 — `Splitter`, `Binder`, `InputPin`,
`OutputPin`, `SubCircuit`, `Constant` — chain them arbitrarily deep. The CPU
census is 34 `Splitter`s, 9 `Binder`s, 43 `Constant`s and 5 `Extend`s of 225
logic elements: **a third of the design is pure wiring, and all of it is on the
priority queue.**

Same document: 318 ns/event, 8,090 cycles/s warm on `riscv/build/k2000.jls`,
**47.7% `PriorityQueue` + `dupCheck`, 37.6% `BitSet`, 4.9% `react()` bodies** —
in-tree, on the real element mix, which is a stronger measurement than
`mech-compiled-backends.md`'s synthetic harness for the same conclusion. And a
measured levelized pass over the CPU's real shape: **4.32 ns/node with plane
arrays, 22.01 ns/node with `BitSet[]`.**

This matters because it names a compiled tier that is **semantically free**:
collapsing the *zero-delay closure* preserves every per-element propagation
delay, because zero-delay elements have no delay to preserve. See conflict C5.

---

## 1. Direct answers to the maintainer's five proposals

### P1 — "allow for nested definition of subcircuits" → **build-with-modifications**

Build the definition/instance split, the elaboration phase, explicit port
binding, and total path addressing; `lf-01` already records this as a program and
`SubCircuit.copy`/`doImport`/`loadElementItems` already elaborate by copy, so
elaboration is an existing implicit phase being given inputs rather than a new
concept. Two modifications: instances **cannot** share runtime element objects —
`Simulator.post` does `dupCheck.add(event)` on a `HashSet<SimEvent>`
(`src/jls/sim/Simulator.java:26, 165-170`, verified), so one instance's pending
event would silently suppress another's identical-time event, and
`Circuit.subElement` (`Circuit.java:50`) is a *single* back-pointer that
`InputPin.initSim` and `OutputPin.react` dereference — and **parameters that
change the port set must not ship in the same round**, because `SubCircuit.init`
(`src/jls/elem/SubCircuit.java:196-271`) derives port set, order, bit width,
y-position and drawn height from the definition's pin set with no rebinding
mechanism, against 142 `getWidth()`/`getHeight()` sites in `src/jls/edit/`.

### P2 — "some switch to toggle between … full fidelity vs compiled and optimized" → **build-with-modifications**

Build it, per **instance** rather than per definition, because holding the design
and the program fixed while changing exactly one boundary is what turns parity
from an assertion into an experiment — and because a *behavioral* implementation
(one `react()`, one lumped delay) is structurally identical to `Adder`
(`30 × bits`) and `Memory` (`accessTime` 100, `docs/simulation-semantics.md:277,
284`), which JLS has shipped since day one with no mechanism asserting they agree,
so the toggle generalizes something already normative rather than introducing a
second execution strategy. The modification is that the maintainer's framing —
"gate and wire level operation at full fidelity" — misnames the axis: JLS is
already word-level, not gate-level (BRIEF §1), so what a user gives up is not
resolution but per-element transport delay, glitches, internal event ordering and
internal observability, and the toggle must say that in those words.

### P3 — "self-contained at a Java level, but maybe even a compiled HDL, Verilog, or SystemC delegated program" → **build-something-different**

The maintainer's own stated preference (self-contained at the Java level) is the
correct one and the alternative should be struck as a *runtime* path: an external
Verilator/SystemC delegate turns the single offline jar into a per-student
toolchain install, and Verilator is 2-state with no counterpart to JLS's
tri-state resolution (`src/jls/elem/WireNet.java:443-527`) or its
two-states-plus-HiZ domain, so every divergence becomes permanent explanation
debt against a project with a written, repeatedly-applied active-maintenance
policy (`docs/library-survey-2026-07.md` rejects JavaHelp, AssertJ-Swing, jqwik,
Cacio-tta, JGraphX and the ANTLR Verilog grammars on maintenance grounds alone).
What to build instead is not a compiler at all in the first round: 47.7% of loop
time is `PriorityQueue`+`dupCheck`, 37.6% is `BitSet`, and **82.3% of all events
are same-timestamp `PinChanged` notifications that model no time**
(`keystone-c-performance.md`, in-tree, on the real CPU) — so a bucket queue, a
plane/`long` value lane, and a zero-delay-closure collapse are together the large
win, all three semantically free, with "no golden changed" as a complete
acceptance criterion.

### P4 — "the in-program definition of elements gets expanded" → **build-with-modifications**

Expand it, but note that ~80% of the construction API is already public and
tested — `Circuit.addElement` (`src/jls/Circuit.java:342-347`, verified public),
the four typed `Element.setValue` overloads, `new Wire(WireEnd,WireEnd)`, and
decisively `jls.Util.partition(Circuit)` (`src/jls/Util.java:145-207`), a public
tested net-former that needs no text — so the deliverable is `lf-07`'s `jls.api`
façade plus a determinism contract, validation and tests, not a subsystem. Two
modifications: **unseal nothing** (measured, `src/` has exactly one type-pattern
switch over the element hierarchy, `src/jls/JLSStart.java:679-684`, and it has a
`default` arm, so sealing buys zero exhaustiveness today and therefore costs
nothing to keep — devices get a sealed intermediate like the existing
`Gate`/`Group`/`Pin`/`SigSim` pattern), and the single highest-leverage change
here is not a mechanism at all but correcting `ARCHITECTURE.md:115-145`'s stale
16-step ritual plus promoting `PaletteContractTest.NON_PALETTE_TAGS` into an
`ElementType.visibility()` field, which takes a headless-only element from ~13
steps to 5 for every future element.

### P5 — "`.jlsx` … internally like json or XML compressed with zstandard" → **build-something-different**

**This is the one place where I disagree with the maintainer plainly, and the
numbers are not close.** The requirement *behind* the proposal — D3, internal
per-section versioning — is real, is binding, and is built here as `FORMAT 3`
section framing in the **existing** line-oriented grammar; but every named
property of the proposal is refused on measurement: compact JSON is 7.0% smaller
plain and **1.3% larger under XZ** (23,416 vs 23,108 B on `p10000.jls`), XML is
+33.7% plain / +2.3% XZ, neither can be *skipped* without being parsed — which is
the one thing a structured container was supposed to buy — and JSON numbers
cannot carry JLS's unbounded `Int` items (RFC 8259 §6; `Constant`'s value is an
`Int`). On zstandard, checked rather than assumed: `zstd-jni` 1.5.6-9 is a
**7,329,564-byte jar bundling 18 native libraries with no musl entry** against a
2,608,994-byte JLS jar, and **both** pure-Java implementations (`aircompressor`
2.0.3 and `aircompressor-v3` 3.7) **hard-fail in both directions under
`--sun-misc-unsafe-memory-access=deny`** — the flag whose default the JDK is
moving to `deny` — while the incumbent `org.tukaani:xz:1.12` (`pom.xml:63-65`,
verified) runs clean and **beats zstd by 36% on binary payloads** (784,344 vs
1,069,399 B on a real 2.49 MB RV32 kernel), which is the content class the
successor exists to serve.

Three further points that should be said rather than softened:

1. **The size problem is not an encoding problem.** Structural sharing is worth
   **11.9×–16.6×** on a 16-instance file (`rep1` 120,233/6,012 B → `rep16`
   1,990,156/71,284 B). Every encoding and compression choice available is worth
   **±7%**. A format redesign justified on size would be justified on nothing.
2. **D1 contradicts the proposal directly.** The maintainer decided uncompressed
   is the default saved format; a zstd-compressed successor optimizes the path
   most files will not take.
3. **`.jlsx` is cosmetic and slightly harmful.** `docs/file-format.md` §1 is
   normative that containers are distinguished by content sniffing, never by
   file name, and the extension's only real effect is to replace JLS's good
   diagnostic — *"this file is save-format version 3 … upgrade JLS to open this
   file"* (`src/jls/Circuit.java:765-771`) — with the OS's "unknown file type".

What *is* worth a distinct extension: `.jlsdata` for sidecar blobs and `.jlsck`
for checkpoints. Those really are different artifacts and the name carries
information.

---

## 2. Conflicts between the designs, adjudicated

### C1 — Can elaboration share structure (element mutable state)? **No. Share the definition; materialize the instances.**

`mech-nested-subcircuits.md` §4.3 says no and prices it at ~35 files;
`mech-fidelity-toggle.md` says per-instance independence is exactly what makes
the toggle free; `mech-compiled-backends.md` says flattening is trivial *because*
instances are distinct; the adversary agrees sharing buys zero speed.

**Ruling: no runtime object sharing, and the binding rule is stated as an output
type — the elaborator's output is the existing runtime `Circuit`.** Reasons:
(i) `Simulator.dupCheck` is a `HashSet<SimEvent>` and `post` is
`if (dupCheck.add(event))` (`Simulator.java:26, 165-170`, verified) — sharing an
element makes instance A's pending event suppress instance B's identical-time
event, which is wrong simulation, not aliasing; (ii) `Circuit.subElement`
(`Circuit.java:50`) is a single back-pointer that cannot hold N values;
(iii) 14 element classes plus `Put.currentValue` and `WireNet.value` hold
per-instance state in their own fields; (iv) the payoff is heap only, and
measured throughput is flat in circuit size (R ~ L^-0.12), so heap does not
convert to speed. Fixing the output type to `Circuit` is what keeps `Simulator`,
`BatchSimulator`, VCD export, HDL export and the GUI working unchanged, and it is
what keeps decision #221 untouched by this mechanism.

### C2 — Is checkpointing a format problem or a sidecar problem? **Sidecar, with one state encoding promotable to a section.**

`mech-format-successor.md` designs it as an optional `state` section but its own
open question §12.1 leans "separate `.jlsck` by default"; the adversary says
sidecar "and it is not close."

**Ruling: sidecar first (`JLSSTATE 1`), and the state *encoding* is designed once
so that the identical bytes later become the body of an optional `state`
section.** Reasons: it ships before `FORMAT 3` and unblocks the multi-hour run
everything else depends on; D1's own content-kind table already assigns
checkpoints to "sidecar file or separate raw section, never diffed"; it decouples
two independently risky things (a state contract over 35 element types, and a new
reader layer on the critical path of every load); and it defers the
`stateHash`/`documentHash` fork until something needs it. Sidecar-first is
strictly dominant because it composes.

**Precondition attached:** the sidecar keys state by instance path + `sid`, and
`mech-nested-subcircuits.md` §1.5 *measured* two elements in two instances of one
definition with identical `getFullName()` **and identical `sid`**. So C2 depends
on C-spine item I1, and separately on the `sid`-minting collision recorded at
`diff-vcs-reality.md:41` (reproduced there by `Collide.java`; I did not
re-verify it this session). Both are preconditions, not follow-ups.

### C3 — How far may the fidelity toggle deviate from `docs/simulation-semantics.md`? **Split the implementation set by whether it engages #221.**

`mech-fidelity-toggle.md` argues the toggle is a model change so #221 is
untouched; `mech-compiled-backends.md` argues #221 must be amended and its
criterion as written cannot be satisfied; the adversary distinguishes behavioral
from levelized.

**Ruling: both are right about different implementations, and the set must be
split accordingly.**

- **`behavioral`** — a hand-written implementation with one `react()` and a
  lumped delay. Does **not** engage #221: `Adder` at `30 × bits` and `Memory` at
  `accessTime` 100 have always been exactly this, and
  `docs/simulation-semantics.md:285` already assigns the subcircuit boundary a
  delay of 0. One normative edit (make that `0` conditional) plus a new §13
  specifying the boundary contract, the observation function, the refusal set and
  the enumerated losses. **A new ARCHITECTURE decision, not an amendment to #221.**
- **`levelized`** — derived automatically from the drawing, replacing internal
  event ordering with an evaluation order. **Does** engage #221's equivalence
  criterion ("per-element propagation delays (§6, §7)", `ARCHITECTURE.md:355-368`,
  verified), which no levelized pass can satisfy as written. Requires, in the
  recorded order: file the follow-up issue that "deliberately does not exist
  yet"; amend the revisit trigger, which names the `riscv/` trajectory and is
  therefore about to name a deleted directory; land the §13 amendment; then code.

This dissolves most of the apparent disagreement and lets the toggle's *contract*
and its *harness* ship before any fast path exists.

### C4 — Combinational cycles under a compiled pass: refuse, or partition? **Partition. The recorded program wins.**

`mech-compiled-backends.md` §3.2 and the adversary both say "reject, never
approximate", and the adversary's sharpest argument against levelization is that
a cross-coupled NAND latch is a first-year exercise with no fixed point.

`lf-02-compiled-evaluation.md` §2.4 already answers it: **Tarjan SCC over the
combinational graph; every non-trivial SCC collapses to one *irreducible block*
node in the level order; outside blocks, straight-line evaluation; inside a
block, iterate to a fixpoint with a bound (`64 · |block|`), and on hitting the
bound report an oscillation naming that set of elements** — a better diagnostic
than today's silent hang. It states the reason plainly: refusal "would opt the
entire sequential unit of a course out of the fast engine", and the `riscv/` GUI
CPU is itself "wired into two feedback loops".

**Ruling: partition.** The adversary's argument is half right — it kills *naive*
levelization, not the recorded design. A latch does have a fixed point (its
settled value); a metastable pair does not, and the bound fires. `lf-02` also
states the honest limit that must go in the normative doc: inside an irreducible
block the fixpoint reproduces settled value, not timing.

### C5 — Which compiled mode ships first? **Mode T (timed-levelized), not Mode C.**

`mech-compiled-backends.md`'s levelized strategy is a whole-run replacement that
turns `now` into a cycle counter and discards §7's delay table — i.e. Mode C.
`lf-02` §2.5 specifies a different first mode: **Mode T — timed-levelized.
Default, always on, no flag, no semantic change. The event queue stays the sole
authority on time; the compiled pass owns only the zero-delay closure.**

**Ruling: Mode T first, and it is the single best-evidenced item in the whole
set.** With 82.3% of all events being same-timestamp `PinChanged` notifications
(§0.4), collapsing the zero-delay cone into one straight-line sweep and posting
only the genuinely timed successors preserves every per-element propagation delay
by construction — so it does **not** engage #221's criterion, needs no doc
amendment, and its acceptance criterion is literally "no golden changed".
Mode C stays deferred behind the C3 process gate.

### C6 — Who owns the sync / retirement-index type? **One type, owned by the equivalence harness.**

`mech-element-definition.md` §5.4 needs a retirement index for record/replay;
`mech-fidelity-toggle.md` §4.4 Tier B needs a declared sync net. Both authors
flagged the collision. **Ruling: one type, one name, one implementation, owned by
the parity/equivalence harness and consumed by the device seam** — because the
harness is the more demanding consumer and because two incompatible notions of
"the point at which the two tiers are compared" is precisely the defect the
parity contract exists to prevent.

### C7 — Where does the `behavioral` implementation actually live? **Both places, and the CPU case is an element.**

An unresolved hole across the set: `mech-fidelity-toggle.md` models the toggle as
selecting among a *sealed set of implementations of a definition*, but a
behavioral RV32 core is not derivable from any drawing — it is a hand-written
model of one particular machine. `mech-fidelity-toggle.md` §8.4 and
`mech-element-definition.md` §8 each half-noticed this.

**Ruling:** the `impl` attribute selects among implementations **derivable from
the definition** (`structural` now, `levelized` later). A hand-written behavioral
macro-element is a **registered `ElementType`** — the precedent `RegisterFile`
already set in-tree (§0.2) — and the harness therefore compares **two `Dut`s**,
not two impls of one definition. This costs nothing, and it is what the parity
study actually needs.

### C8 — `FORMAT 3` framing unit: line counts or token counts? **Line counts.**

`mech-format-successor.md` proposes `SECTION name version lines`;
`mech-adversary.md` prototyped a token count. **Verified the deciding fact:**
`docs/file-format.md` §2 makes it a **MUST** that "a quoted value MUST begin and
end on the line of its item; embedded newlines are escaped", and the canonical
layout is one record per line. So a line count is safe, and unlike a token count
it is checkable by a human hand-editing the file.

Both designs agree on the load-bearing half and it must be written into the spec:
**the declared count is authoritative; a reader that understands a section
asserts it consumed exactly that many lines; a mismatch is `MALFORMED` naming the
section and both counts, and a reader must never resynchronize** — because a
resynchronizing skip is how forward compatibility turns into silent corruption.

### C9 — Blob storage: inline base64 section, or `blobref` + sidecar? **`blobref` + sidecar.**

The adversary measured a 16 MiB image as 22.66 MB of inline base64, under the
64 MiB cap. `mech-format-successor.md` measured that a fully-populated 16 MiB RAM
as `init` text is **53.7 MB, 84% of the entire cap**, and that base64 is 1.333×
raw. **Ruling: `blobref` + `.jlsdata` sidecar**, because D1's content-kind table
already assigns memory images to a sidecar, and because the adversary's own cost
note concedes "at three or four such images a circuit hits the cap. I would take
the sidecar." Digest verified on every read; missing blob fatal by default.

### C10 — Three designs each claim the O(W²) partition fix. **One owner.**

`Circuit.finishLoad:1355-1395` (`:1369` is `LinkedList.remove`) and
`Util.partition:145-207` (`:172`) are the same shape and the same bug.
`mech-nested-subcircuits.md` slice 0, `mech-element-definition.md` §2.7 and
`mech-format-successor.md` §2e all schedule it. **Ruling: it lands once, in
`jls.api`'s net-former, with `Util.partition` and `Circuit.finishLoad`
delegating.** It is landed *before* elaboration so elaboration is neither
credited nor blamed for it (measured today: 196.0 ms at 12,960 wire ends; 8× the
ends costs 33× the time).

### C11 — Is the construction API new? **No — build `lf-07`'s `jls.api`.**

Four proposals and one mechanism design independently reinvented it. **Ruling:
build the recorded one.** Its determinism prerequisite from
`mech-element-definition.md` §2.6 is real and must be carried in:
`Pin.init` only sizes when `g != null`, so a headless builder passing `null`
produces `width 0` and a build→save→load→save cycle is not a fixed point.
A deterministic AWT-free `TextMetrics` is a slice-1 requirement, not a polish item.

### C12 — Is the toggle or the constant-factor work the primary speed lever? **Constant factor, and it is not close.**

`mech-fidelity-toggle.md` §8.3 and `mech-compiled-backends.md` §1.5 both concede
this; the adversary ranks it highest. The in-tree measurement settles it:
47.7% `PriorityQueue`+`dupCheck`, 37.6% `BitSet`, **4.9% actual `react()`
bodies** at 318 ns/event on the real CPU. **Ruling: the toggle must not be sold
as a speed mechanism.** Its unique and irreplaceable value is that it makes
parity a property of a **boundary**, which nothing else in this set provides.

---

## 3. The dependency spine

Ordered. Each item names what it unlocks. `→` is a hard precedence.

```
F1  tag conformance anchored on ElementRegistry          (fixes a live defect; gates all format work)
 →  R0  re-home riscv/-dependent tracked assets           (precondition of the deletion)
 →  E1  bucket/index queue replacing PriorityQueue+HashSet   ─┐ semantically free;
 →  E2  plane/long value lane replacing BitSet (<=64 bits)   ─┤ "no golden changed"
 →  E3  Mode T: zero-delay-closure collapse                  ─┘ is the whole oracle
 →  A1  jls.api construction API + FixedTextMetrics + the O(W^2) partition fix
 →  I1  instance identity + ElementPath/ElaboratedId addressing   (NO format change)
 →  F2  FORMAT 3 section frame, shipped with only the Circuit section
 →  X1  elaboration phase + LIBRARY/DEFINE as the `library` section
 →  H1  boundary-equivalence / parity harness + THE sync-net type
 →  T1  per-instance fidelity toggle, sealed set {structural, behavioral}
 →  M1  the behavioral machine model (Rv32Model + Rv32Core)
 →  V1  device seam: ConsoleDevice out, then in with record/replay
 →  K1  checkpoint sidecar (JLSSTATE 1) keyed on ElementPath
```

Precedence facts, each with its reason:

- **F1 before F2.** Adding a second versioning layer on top of an unenforced
  totality invariant is the move §0.2 forbids.
- **R0 before the `riscv/` deletion**, not before anything else — but it blocks
  the deletion, and the deletion is already decided.
- **E1/E2/E3 before everything measurable.** They change the constants every
  other decision is evaluated against, and they are the only items whose
  correctness oracle is complete and mechanical.
- **A1 before X1.** Elaborating a definition into instances *is* a construction
  program; `lf-02` §1.1 states the general form — *"there is no elaboration step,
  so there is nowhere to put a compiled netlist."*
- **I1 before K1, H1, V1 and the VCD fix.** Everything downstream must be able to
  **name** state inside a nested instance. Today two watched registers in two
  instances of one definition collapse into one VCD signal
  (`BatchSimulator.toVcd:394-399`), measured.
- **F2 before X1's persistence.** `FORMAT 3` may be spent on **the frame and
  nothing else**; otherwise `blobref` needs FORMAT 4 and `state` needs FORMAT 5,
  which is exactly the failure D3 names. Every later feature is a `Circuit`
  section version increment or a new section.
- **H1 before T1.** Ship the differential harness before the thing it is supposed
  to check, so the toggle's contract cannot pass vacuously.
- **T1 + M1 before any parity claim.** The toggle supplies a boundary; M1 is the
  only thing on the other side of it.

---

## 4. The keystone

**The elaboration phase with total instance addressing (`I1` + `X1`) — a
definition/instance split whose elaborator output type is the existing runtime
`Circuit`, plus `ElementPath`/`ElaboratedId` as the single addressing scheme.**

It unlocks the most because it is the only mechanism the others must *name things
through*:

- the checkpoint sidecar keys state by it (C2);
- the parity trace names architectural state by it (`/cpu/regfile`, `/cpu/pc`)
  instead of by search heuristics;
- the VCD collision defect is fixed by it, and only by it;
- the per-instance fidelity toggle needs a per-instance identity to attach to;
- the device seam binds channels through it;
- and `lf-02` §1.1 states the strongest form: **"There is no elaboration step, so
  there is nowhere to put a compiled netlist"** — the compiled tier has no home
  without it.

Two honest qualifications. First, **keystone ≠ first**: the highest
value-per-burden item is the constant-factor engine work (E1/E2/E3), which
unlocks nothing structurally but changes every constant and has a complete
oracle. Second, **`A1` weakens `X1`'s urgency for the boot goal specifically**:
once a ~600-element machine is generated by a `jls.api` program, edit-once-
update-all matters much less, because the generator is the single source. The
16.4× file-size win and the ~10–20× load-time win remain, and the *addressing*
half remains mandatory — but definition **sharing** is primarily an authoring
feature for humans, which is the actual product and not the boot.

---

## 5. The minimum credible version

The sentence to make true: *a terminal-only Linux boots on JLS-simulated
hardware and can be interacted with, with parity between virtual logic and
virtual hardware.*

Taking the adversary seriously means accepting its central reading: **live
interaction belongs to the behavioral tier; parity is offline** (BRIEF §6 — all
timing and all microarchitectural state are permitted to differ). That is what
lets the MCV omit levelization, codegen, and every external tool.

But the adversary's own §8 minimum does **not** make the sentence true: it has no
device seam and no machine model, so there is no terminal and nothing to boot. It
priced the *mechanisms* and not the *machine*. The MCV below corrects that.

| # | Item | Why it is minimum |
|---|---|---|
| 1 | **F1** — `ElementRegistry`-anchored tag conformance; add the two tags to `SaveTags` and spec §7 | A live spec/code divergence that CI misses (§0.2). ~15 lines. Precondition for trusting any format work. |
| 2 | **R0** — re-home `RiscvCpuGoldenTest`'s regeneration path, `k2000.jls`, and the `riscv_ref.py`/`fuzz_diff.py` differential design | Two **tracked** test assets are orphaned by the deletion (§0.3). |
| 3 | **E1 + E2 + E3** — bucket queue, plane/`long` value lane, Mode T zero-delay closure | Turns the structural parity run from hours into a nightly job, i.e. from an oracle that rots into one that runs. 47.7% / 37.6% / 82.3%, measured in-tree. Zero semantic change. |
| 4 | **A1** — `jls.api` + `FixedTextMetrics` + the O(W²) partition fix | A ~600-logic-element machine is not hand-drawable, and this is the in-tree tested replacement for the deleted `riscv/` generator (D5 requires one). |
| 5 | **I1** — instance identity + `ElementPath`/`ElaboratedId` | Fixes the measured VCD collision; is how every later item names state. No format change. |
| 6 | **F2** — `FORMAT 3` section frame, `Circuit` section only | **D3 is binding.** Spend the last global bump once, on the frame, correctly. |
| 7 | **M1** — the behavioral machine: `jls.mach.rv32.Rv32Model` behind a `jls.elem.Rv32Core` element | The "virtual hardware" half of the sentence, and the **only** tier that can be interactive (BRIEF §5: 1e5–1e6 cycles/s needed vs 2–23 events/cycle of budget). ~3 min boot per BRIEF §4. |
| 8 | **V1** — device seam: `ConsoleDevice`, poll-based, output then input, record/replay indexed by the sync net | Without it there is no terminal at all. BRIEF §7 lists "no host I/O and no device concept" as **fatal**. Devices must never inject events — self-scheduled `sim.post` from inside `react()` only. |
| 9 | **Clint + Multiplier + Divider + `ElementType.visibility()`** | BRIEF §3: the boot is compute-bound *provided CLINT `mtime` is driven by SIMULATED time* — running the machine 100× slower cost only 8% more instructions. M/D removes ~190 elements and ~70 ev/instruction. |
| 10 | **H1 + T1** — the parity harness with a deliberately-failing null test, and the per-instance toggle over `{structural, behavioral}` | This is the word "parity". Without the null test the harness can pass vacuously. |
| 11 | **K1** — checkpoint sidecar keyed on `ElementPath` | A multi-hour structural run that cannot be suspended cannot run on any lane (141 s required gate, 6 h hosted ceiling). |
| 12 | **The time-limit ceiling** — `JLSInfo.defaultTimeLimit = 100000000` (`src/jls/JLSInfo.java:69`) | A boot run exceeds it. And a self-scheduling device makes the dropped-past-`maxTime` event (`Simulator.java:224-234`) observable, so BRIEF §10's open question must be adjudicated before item 9 ships, not after. |

Item 7 is the largest uncosted piece in the entire study. It is designed in
`prop-behavioral-first.md` (`jls.mach.rv32.Rv32Model` as a pure
`CommitRecord step(ArchState, MemoryView)`, with `Rv32Core` as a ~200-line
adapter) and owned by **none** of the six mechanism documents. Naming it is the
most important thing this integration does.

**Everything else is deferred** — see §7 below.

---

## 6. The test that makes each mechanism enforced rather than aspirational

One per mechanism. Failure of this test means the mechanism has decayed.

| Mechanism | The enforcing test | What it pins |
|---|---|---|
| **F1** tag conformance | `FileFormatSpecTest.everyRegisteredTypesWrittenTagIsFrozenAndDocumented` — iterate `ElementRegistry.ALL`, save one instance of each, assert the emitted `ELEMENT <tag>` is in `SaveTags.writableTags()` **and** in spec §7 | **Fails on HEAD today.** Closes the doc↔`SaveTags`↔`ElementRegistry` cycle at the table the reader actually uses. |
| **E1/E2** constant factors | `EngineEquivalenceTest.optimisedQueueRetiresTheIdenticalEventSequence` over a corpus **including a cross-coupled latch, a tri-state bus and a zero-delay ring**, plus every existing golden byte-identical | Constant-factor work is only free if this holds; the oracle is complete and mechanical. |
| **E3** Mode T | `ModeTGoldenTest.everyGoldenIsByteIdenticalWithZeroDelayClosureEnabled` — full VCD **and** stdout byte-for-byte | The acceptance criterion *is* "no golden changed". If this ever needs a baseline update, Mode T has changed semantics and must stop. |
| **A1** construction API | `BuilderLoaderEquivalenceTest.builtCircuitHashesLikeTheLoadedOne` + `BuildDeterminismTest.buildSaveLoadSaveIsAFixedPoint` under perturbed font, locale and TZ | Makes the API byte-equivalent to the loader, which is the only reason it is testable at all; the fixed-point test is what fails if `FixedTextMetrics` is skipped. |
| **I1** addressing | `VcdSignalCollisionTest` — two instances of one definition, each with a watched register, produce **two** VCD signals; plus `ElementPathUniquenessTest` asserting `ElementPath.of` is injective across the elaborated tree | **Fails on HEAD today** (measured collision). Turns a live defect into a regression pin. |
| **F2** framing | `SectionFrameTest.aSectionsDeclaredLineCountIsAuthoritative` (mismatch ⇒ `MALFORMED`, never resynchronization) + `.unknownOptionalSectionIsSkippedWithADiagnostic` + `.unknownRequiredSectionIsRefusedAsNewerFormat` + `FormatEvolutionTest.formatVersionDoesNotMoveWhenASectionIsAdded` | The last test pins D3 itself: if it ever needs updating, the frame has failed at its one job. |
| **X1** elaboration | `ElaborationDeterminismTest` — an N-instance definition file yields a **bit-identical simulation trace** to the equivalent N-inline-copy FORMAT 1/2 file; plus `LegacyInlineSubcircuitGoldenTest` (byte-identical re-save of every existing fixture) and `ElaborationBombTest` (a <4 KB file expanding 4⁸ refused at `MAX_ELABORATED_ELEMENTS`) | The differential oracle against today's behaviour, plus the new decompression-bomb class that definition sharing creates and the inline format structurally cannot express. |
| **H1** parity harness | `FidelityParityTest.structuralAndBehaviouralAgreePerRetiredInstruction` **plus `FidelityParityTest.nullTestFails`** — a deliberately divergent behavioral implementation the harness must catch | Without the null test a differential harness passes vacuously. PIT will find survivors in comparison logic, so assert the **diff report text**, not the boolean. |
| **T1** fidelity toggle | `FidelityManifestTest.allStructuralRunIsByteIdenticalToTheGolden` + `SoleStrategyTest.implementationsNeverTouchTheEventQueue` (bytecode scan: `Simulator.post` allowed; `eventQueue`, `dupCheck`, `poll`, `runEventLoop` forbidden) | The second test is the mechanical proof that the toggle is a model change and not a second execution strategy — a fact rather than a claim. |
| **M1** behavioral machine | `Rv32ModelConformanceTest` against the re-homed reference emulator + the tracked `RiscvCpuGoldenTest` fixture | The model is the reference half of the parity contract; it must itself be checked against something that is not JLS. |
| **V1** device seam | `RetirementIndexTest.replayIsInvariantUnderClockPeriodChange` — replay the same log with the `Clock` period doubled; guest output bytes identical while simulated time differs. Plus `NoEventInjectionRatchetTest` and `ChannelModePolicyTest` (CI never runs `LIVE`) | Makes BRIEF §6's "all timing may differ" executable, and fails if anyone quietly reindexes the log in nanoseconds. |
| **K1** checkpoint | `CheckpointSidecarTest.restoreResumesTheIdenticalEventStream` (N, checkpoint, restore, M ≡ straight-through N+M) + `.staleSidecarIsRefusedNotSilentlyApplied` | Silent application of a stale checkpoint is the failure that would corrupt every parity result downstream. |

Cross-cutting, already in-tree and must stay green through all of it:
`ContainerMutationFuzzTest`, `GenerativeRoundTripFuzzTest`,
`UntrustedFileHardeningTest`, `StableElementIdTest`, `DeterministicSaveTest`,
`HeadlessCoreRatchetTest`, `SocketConfinementRatchetTest`,
`ExtensionPointCatalogTest`.

---

## 7. What this set does NOT solve

**Deferred deliberately** (each recoverable, none in the MCV): subcircuit
parameters of every kind — value, width, port-count — and bounded parameterized
recursion; external `.jlslib` libraries; the Mode C levelized evaluator; codegen
of any kind (`javax.tools` returns `null` on the `jlink` image
`scripts/build-installer.sh:143-145` ships, and there is a measured 68× cliff at
HotSpot's 8000-bytecode `HugeMethodLimit`); Verilator and SystemC as runtime
paths; `.jlsx`, JSON, XML and zstd; `blobref` + `.jlsdata` sidecars; the in-file
`state` section; `BlockDevice`; and flipping `DeviceElement` to `non-sealed`
(gated on #212's demand gate, which has not opened).

**Genuine gaps this set leaves open:**

1. **`alpha`, the per-cycle active fraction, is still unmeasured** — 3.1× spread
   (0.18 / 0.40 / 0.56), and it is the dominant uncertainty in the boot model
   *and* the parameter that decides Mode C's margin. The cheapest experiment
   remains the one BRIEF §10 names: convert the single-cycle demo into a 2-cycle
   unified-memory machine (~10 elements) and count events with an internal
   `Clock`. **Nothing in this set is blocked on it, and nothing in this set
   measures it.** It should be authored inline against today's format,
   immediately, in parallel.
2. **The single-source premise `D` of the parity contract has no mechanism.**
   BRIEF §6 posits one machine definition `D` from which both `M_H` and `M_L`
   derive. The toggle gives a *boundary*; `jls.api` gives a *generator*; nothing
   in this set makes the behavioral model and the structural drawing derive from
   one artifact. Today they would be two hand-maintained things asserted to agree
   by a test. That is workable and it is what the industry does — but it should
   be recorded as a known weakness rather than assumed away.
3. **`docs/vcd-interop.md:19-24` still contradicts `docs/grand-architecture.md`
   and the stated goal.** The device seam dodges it (default `NULL`, CI mode
   `REPLAY`, a pre-recorded artifact), but **interactive** use of the behavioral
   tier does not. It must be explicitly reopened, not quietly overridden.
4. **No CI lane can host this.** The required gate is 141 s, there is no
   `timeout-minutes` anywhere, the hosted ceiling is 6 h, and there is no Git LFS
   or large-fixture policy. Even the post-E1/E2/E3 structural run needs a nightly
   lane that does not exist, and nobody has said who commits the CPU-scale
   fixture or how it is simultaneously CI-fast and evidentially representative.
5. **The `sid`-minting collision** (`diff-vcs-reality.md:41`, reproduced by
   `Collide.java` — a normal load-edit-save cycle can mint a `sid` colliding with
   one already in the file, producing a file JLS then refuses to open). It is a
   precondition of K1's keying and I assumed rather than scoped its fix; I did not
   re-verify it this session.
6. **The O(n²) `SigSim` stimulus parse** — 80% of end-to-end wall time and 95% of
   allocation (39.58 GB of 41.76 GB on a 50k-cycle run). An ordinary bug, sitting
   in **front** of every measurement in this study. Fixing it may matter more
   end-to-end than E1 and E2 combined and it is independent of all of them.
7. **`DenseWordStore` reaches 16 MiB at 2²² 32-bit words with zero headroom**
   against Linux's ≥12 MiB (16 MiB recommended) requirement. One word past the
   limit and the cost class changes. Nobody designed the headroom.
8. **GUI per-edit cost** — 58 ms at 10k elements, 552 ms at 100k (whole-index
   invalidate plus whole-circuit snapshot). Untouched by everything here.
9. **RV32 nommu is on a published deprecation trajectory** — a Feb-2024 patch
   proposed removal "by the beginning of 2027", and RV32 requires
   `CONFIG_NONPORTABLE=y`. That is roughly five months out. The MCV should be
   able to state its hedge (Sv32 + S-mode, +160 elements, ~4.4 h) rather than
   discover the problem.
10. **`Divider` cannot ship without a normative change** fixing division-by-zero
    and signed-overflow results. A divider with unspecified `MIN / -1` is a
    parity-bug generator that will be attributed to the wrong thing.
11. **Live interaction on the *structural* tier remains out of reach.** The MCV
    makes the sentence true with the behavioral tier interactive and the
    structural tier bound to it offline. If the maintainer's actual want is a
    live console on the drawn CPU, Mode C moves from deferred to mandatory and
    §5's shape changes. **This is one sentence from the maintainer and it is the
    highest-leverage unresolved question in the study.**
