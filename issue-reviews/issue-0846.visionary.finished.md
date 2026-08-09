# Issue #846: TASK-C370-3: runtime state moves out of per-element objects into the primitive columns, the element author's contract is unchanged, and every simulation golden is byte-identical
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

#846 is the payload task of FEAT-054 (#370). Everything around it is scaffolding —
#842 measures, #843 builds the index and empty columns, #848 asserts one index,
#850/#851 reconcile the readers. #846 is where the bytes are meant to leave.

The end is worth serving: a generated design (an RV32I core from
`riscv/make_cpu.py`, an imported netlist from #33/#59) should not cost ~1,190 B of
live heap per element, and ≤150 B/element is a defensible thing for JLS to become on
the batch/grading/HDL side. But the task as filed cannot reach that target, and the
reason is visible in the tree without running anything. **I am explicitly
disregarding AC-1 and AC-4 as written**, because they are mutually exclusive at the
byte level, and the design that satisfies both cuts a different seam.

## 1. "Runtime state" is not where JLS's bytes are

A 2-input gate with three attached wire ends, counted from the declared fields at
HEAD (rough live bytes, headers included):

- `Gate`/`LogicElement`/`Element` fields — ~88 B — `Element.java:20-49`,
  `LogicElement.java:24-35`, `Gate.java:44-50`. Saved config and editor geometry.
- `ElementId` object — ~32 B — `Element.java:24` (#165 identity).
- `Vector inputs` + `Vector outputs` + their backing arrays — ~190 B —
  `LogicElement.java:33,35`. Pure structure.
- 3 × `Put` — ~200 B — `Put.java:22-44` (name, xr/yr, savex/savey, bits, touching,
  wireEnd, myCopy, face).
- 3 × `BitSet currentValue` + `long[]` — ~170 B — `Put.java:385`. **This is the
  runtime state.**
- 3 × `WireEnd` + 3 × `LinkedHashSet wires` — ~490 B — `WireEnd.java:24-28`.
- 3 × `LinkedHashSet loadWires` + 3 × `HashMap probeMap` — ~450 B —
  `WireEnd.java:44-48`. **Load scaffolding, consumed once at `WireEnd.java:102-152`
  and then retained for the life of the circuit.**
- `HashMap$Node` in `Circuit.elements` — ~40 B — `Circuit.java:48`.

That composition is what ~1,190 B/element on a CPU and ~2,150 B/element on a
wire-heavy chain look like. The share that is *runtime state* — what `react` reads
and writes — is the per-put `BitSet` plus a few element-local fields
(`Register.toBeValue`/`currentC`, `Memory` contents, `StateMachine` state, `Clock`
phase). Call it 200-300 B of 1,190.

**Moving 100% of the runtime state into columns removes at most ~20% of the
footprint.** Deleting the dead load scaffolding removes more, for a day's work and
no contract change at all. The 8-14× lives in object *count* and structural
collections — exactly what #370 §3's own formula says, dominated by $r = 6.8$ and
$h = 12..16$. You do not reduce $r$ by moving fields. You reduce it by deleting
objects.

## 2. AC-4 forbids deleting the objects that hold the bytes

`Register.react` (`Register.java:751-793`) reads `inputs.get(1).getValue()`,
`inputs.get(0)`, `outputs.get(0).setValue(...)`, `propDelay`, `type`. For that body
to compile unchanged — AC-4, demonstrated with a real element — `inputs` must stay a
`Vector`, its members must stay `Input` objects, and `Put.getValue()` must stay a
method on a live object. So `Element`, `Put`, both `Vector`s and their arrays (~480 B
of the ~1,190) are **guaranteed to survive by acceptance criterion**, before
`WireEnd` and its three collections are counted.

AC-1 and AC-4 are therefore jointly satisfiable only in the weak sense:
`Put.currentValue` becomes an `int` index into a long column and the getter reads
the column. Legitimate — and roughly what #322 (FEAT-026) buys anyway from the value
side, more cheaply — but it is a ~15-20% change being funded and reviewed as an
order-of-magnitude one. AC-3 then records a factor near 1.2 against a target of
7.9-14.3, and K17-1 fires having spent #846's 5-8 mw plus #843's 3-5 mw to learn it.

## 3. The reframing: elaborate, don't migrate

FEAT-054 cuts *inside the element*. The seam that actually exists in this codebase
is *between the document and the machine*. JLS simulates **on the drawing**:
`Element` is at once the editor's model (geometry, snap-to, highlight, attributes,
dialogs, icon, help page) and the simulator's model (`react`, `initSim`, values).
Every problem the feature is fighting — invariant 3, invariant 4, #850's spatial
index, #851's editor view — follows from that conflation, not from field layout.

The alternative: **`Circuit` elaborates a flat primitive netlist at simulation
start, and the simulator runs on that, never on the elements.**

- The netlist is columns: per-node opcode, operand indices, width, delay, value
  words. No `Element`, `Put`, `Vector`, `WireEnd`, `ElementId`, geometry, or
  attributes. $r$ goes to ~0 objects per element — the only way ≤150 B/element is
  reachable *by construction* rather than by hoping the residue fits.
- It is **derived, one-way, disposable**. Invariant 4 is satisfied by derivation
  rather than by a proven view; the editor's graph is untouched, so invariant 3 —
  the criterion #370 says can veto the feature — becomes trivially true instead of
  the one most likely to fail.
- **#850 and #851 largely evaporate**: the spatial index and the editor keep reading
  the document, which is still there and still authoritative for everything they
  care about. Two of the six tasks, and the two hardest.
- The elaborator exists in embryo: `Circuit.finishLoad`, `WireNet.makeNet`
  (`WireNet.java:97`), `LogicElement.initSim` — which is the natural place for an
  element to *emit rows* instead of initializing itself.
- **Capacity and audience line up.** CAP-17-scale designs are generated
  (`riscv/make_cpu.py`) or imported (#33/#59), never drawn. A 10⁹-element design
  should never become an editable document at all: the editor cannot render it,
  `SpatialIndex` cannot index it, and `MAX_CIRCUIT_TEXT_BYTES` (64 MiB at ~96.6
  B/element on disk) cannot hold it. Elaboration gives that path a front door — a
  generator or HDL importer emits the netlist directly. Migrating fields inside
  element objects gives it nothing, because the document is still built first.

The honest cost: this eventually changes the element-author contract (#370 invariant
2). Which is why that invariant is the wrong thing to protect.

## 4. Invariant 2 pulls against the project's own recorded direction

ARCHITECTURE.md's "Adding an element today (the honest list)" enumerates *sixteen*
touch points and closes: "read #78 first; the registry is the recorded direction."
The project has already decided this contract will change substantially. FEAT-054
spends a 12-20 mw feature — and #846 its central acceptance criterion — freezing it.
Whatever shim keeps `inputs.get(0).getValue()` compiling is shim #78 must unpick,
and AC-4's demonstration proves a property the roadmap intends to discard. The
defensible version of invariant 2 is not "the contract is unchanged" but "**the
element author writes one behaviour description and the engine decides where it
runs**" — which a registry plus elaboration delivers and field-shuffling behind a
frozen 1990s interface does not.

## 5. #221 already named this decision, and named a better acceptance test

ARCHITECTURE.md records the discrete-event interpreter as the *sole* strategy, with
"the Verilator/CXXRTL elaborate-to-flat approach" deliberately not built; revisit
trigger — a CPU-scale design on the `riscv/` trajectory that is unusably slow;
equivalence criterion — bit-for-bit agreement with the #202 RV32I golden as a
differential oracle.

#846 is elaborate-to-flat's *data layout* without its *evaluation model*: the
invasive half, with none of the levelized payoff `keystone-c-performance.md`
measures at 4.32 vs 22.01 ns/node. If the trigger has fired, the recorded response
is to open the #221 follow-up and build the pass properly. If it has not, this buys
a strategy change's cost without the strategy.

That criterion also beats AC-2. A byte-identical golden corpus is a sample; two
representations run side by side and compared event-for-event is a proof — and it is
only available while both exist, which elaboration preserves and migration destroys.

## 6. The gating number is knowable before the work, not after

#846 is irreversible by construction while its own success metric (AC-3, feeding
K17-1) is measurable only after it lands. A prototype elaborator — walk a loaded
circuit, emit columns, size them, throw them away — touches **zero** `react` bodies,
is days of work, and produces the exact number K17-1 fires on. At 90 B/element the
feature is justified and proceeds with confidence; at 400 the capstone is restated
at 10⁸ having spent days rather than two tasks. It also retires #842's awkwardness
of measuring below the load-path wall: a synthesized netlist has no load path.

## What survives every reframing

The dense element index (#843) and the one-index property (#848) — the elaborated
netlist needs exactly that mapping. The insight that layout, not size, buys both
capacity and throughput, and that #370 and #362 must be one purchase; elaboration
makes that literal. And the refusal to accept "kept in step" as a design: derivation
is the strongest form of that refusal.

**Restated task.** Elaborate a loaded circuit into a flat primitive-column netlist
the simulator runs, leaving the element/editor graph untouched and authoritative for
editing. Gate it on a measurement of the elaborated netlist's per-element heap on a
generated RV32I-scale design, taken before any element is migrated. Accept it
against a differential oracle — interpreter and flat pass agreeing event-for-event
on every fixture and on #202's golden — not a golden diff alone.

## Verdict

**rethink.** The end is right; the seam is wrong. As filed, #846 moves the ~20% of
bytes that are runtime state while AC-4 pins the ~80% that are objects, structure,
and load scaffolding, and it spends the feature's central invariant protecting a
contract #78 already intends to replace. Cut between the document and the machine:
elaborate a flat netlist, keep the editor's graph, measure the gating number first,
accept against a differential oracle.
