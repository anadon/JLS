# Issue #370: FEAT-054: a circuit's runtime state lives in flat primitive arrays indexed by element, so the per-element footprint falls by roughly an order of magnitude and the largest design that fits one machine grows by the same factor
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Two things, and they are not the same thing. (a) CAP-17 (#312) needs per-element live
heap at or below 150 B so a 10^10-element design is reachable by partitioning. (b) The
engine programme (#362, #232) needs plane-major arrays because `BitSet[]` costs
22.0 ns/node against 4.32 ns/node for `long[] a,b,u`. The issue correctly identifies
that one layout serves both. It then makes one architectural choice — *flatten the
runtime state of the live, editable object graph, in place, behind an unchanged
element-facing `react` contract* — and hard-codes that choice into § 4 invariants 2 and 4,
which no child may trade away. **That choice is the problem, and the repository already
contains a better one.** I am disregarding invariants 2 and 4, § 2 rows 3 and 4, and
integration criteria 4 and 6 below, for the reasons that follow.

## 1. The footprint is attributed to the wrong cause, and the cheap fix is never considered

The issue's argument is that Java object headers dominate: "12-16 bytes across the
measured 6.8 runtime objects per logic element, dominate before any field is counted."
Run the arithmetic on the issue's own numbers. `r` = 6.8 is not "6.8 state fragments per
element" — the keystone-C census (`docs/capability-roadmap/keystone-c-performance.md` § 2)
reads `elements(all, recursive)=1551` comprising 225 logic elements, **810 `WireEnd` and
513 `Wire`**. 1551/225 = 6.9. The 6.8x *is the wire graph*. At 1,190 B/element and
h = 16 B, headers are **9%** of the footprint; the remaining ~159 B of average fields per
runtime object is where the order of magnitude actually lives. Headers dominate the
*150 B target*, not the *1,190 B baseline*, and the issue conflates the two.

So where are the 159 B? `src/jls/elem/WireEnd.java:24-48` — every wire end eagerly
allocates **three** collections:

```java
private Set<Wire> wires = new LinkedHashSet<Wire>();      // :28  live structure
private Set<Integer> loadWires = new LinkedHashSet<Integer>(); // :44-45  load scaffolding
private Map<Integer,String> probeMap = new HashMap<Integer,String>(); // :47-48  load scaffolding
```

plus `loadAttach`, `loadPut`, `loadTriState`. `loadWires` and `probeMap` are consumed
exactly once, in `WireEnd.init(Circuit)` (`:102`, `:152`), called from
`Circuit.finishLoad` (`src/jls/Circuit.java:1349`) — **and never cleared**. Every loaded
circuit therefore retains, per wire end, a `LinkedHashSet` of boxed `Integer`s and a
`HashMap` of probe names that are dead the instant `finishLoad` returns. At 3.6 wire ends
per logic element, a few hundred bytes of retained load scaffolding per wire end is on the
order of the entire measured per-logic-element footprint.

I have not measured this and say so plainly. That is the point: **neither has the issue.**
§ 2 row 1 measures a per-element *aggregate*; nothing in the decomposition asks where the
bytes go. A `jmap -histo:live` or a JFR object-count sample on `riscv/build/k2000.jls` is
an afternoon and would settle whether a 12-20 mw structure-of-arrays rewrite is needed at
all, or whether nulling three fields and moving load scaffolding to a side table in
`finishLoad` buys most of it for a day's work, with no contract change, no editor risk,
and no dependency on #322/#335/#362. **Row 1 should be a composition histogram, not a
scalar.** A feature whose entire justification is "the footprint is N" cannot responsibly
skip "and here is the breakdown of N."

## 2. The seam is wrong, and `lf-02` already cut the right one

`docs/capability-roadmap/lf-02-compiled-evaluation.md` § 1.1 states the same structural
fact this issue states — "There is no array of anything" — and reaches the opposite
design. § 2.1 specifies `jls.core.sim.Netlist`, built by a `NetlistBuilder` that flattens
`SubCircuit` hierarchy, unions nets across jumps (lifting the `UnionFind` walk that already
exists at `src/jls/hdl/HdlExporter.java:1038-1109`), assigns dense ids, keeps a
bidirectional `nodeId ↔ Element/WireNet` map, emits ~12 opcodes covering >97% of the event
mix, and provides an ESCAPE shim so the other 13 `react` bodies are called unchanged. The
state layout it names is exactly the one this issue wants: `long[] a,b,u; int[] width;
byte[] opcode; int[] order`.

The difference is one word: **derived**. `lf-02`'s arrays are *produced from* the drawn
model by a pure function at Run time. #370's arrays *replace* the drawn model's state.
That single distinction dissolves most of this issue:

- **Invariant 4 ("no second representation maintained by discipline") becomes vacuous.**
  A derived netlist is not a second representation held in step by discipline; it is a
  build product with a rebuild trigger. Agreement is a compilation property, not a runtime
  invariant. § 2 row 4 — which the issue itself calls "the largest source of latent
  divergence" — **ceases to exist**. The editor keeps reading the object graph it has read
  for twenty years.
- **Invariant 3 (the pedagogy veto on per-edit cost) becomes unreachable by
  construction**, not something integration criterion 3 has to hunt for. Nothing on the
  edit path changes. The issue names invariant 3 as "the criterion most likely to fail the
  feature"; a derived netlist removes the failure mode rather than testing for it.
- **Row 3's "one index shared by the columns and the event queue"** stops being a
  cross-feature treaty. The `Netlist` *owns* the id space by construction; the queue
  indexes into it. This is precisely the #848 ↔ #476 collision the 2026-08-08 boundary
  comment identifies: an index contract written against a `PriorityQueue` that #476
  deletes. Under elaboration there is no contract to write — there is a producer and a
  consumer.
- **The #846 ↔ #879 collision likewise evaporates.** Two features cannot both relocate the
  same runtime state if neither relocates it: #879 widens the value type in the document
  model, the elaborator lowers whatever the document model holds into planes. They compose
  instead of colliding.

The collisions in that comment are not scheduling bad luck. They are what in-place
flattening *is*: a rewrite of structures three other features are simultaneously
rewriting. New code in a new package collides with nothing.

## 3. The 10^10 user never opens the editor

CAP-17's target design is on the order of 10^10 gates. Nobody draws that. It arrives from
Verilog import / Yosys JSON (`docs/hdl-support-research.md`, #33/#59) or from a generator
like `riscv/bench_kernel.py`, and it is observed through VCD and batch reports. FEAT-055
(#332) already names its own scope as "partitioned model and **streaming elaboration**" —
it presupposes an elaboration phase that #370 declines to build. The capacity capstone's
actual load path wants: netlist source → streamed elaboration → column arrays → simulate
headless, with the drawn `Element` graph **never instantiated at all**. #370 as written
guarantees the opposite: to get 10^10 columns you must first construct 10^10 `Element`
objects and then flatten their state, which is the very allocation the capstone cannot
afford. Under an elaboration seam, `MAX_CIRCUIT_TEXT_BYTES`, the quadratic `finishLoad`
partition (`Circuit.java:1345,1368-1369`), and the `SpatialIndex` rebuild — three of the
four measured walls, and the two the issue explicitly disclaims — are all *editor-path*
costs that a headless elaborated run never touches. The issue treats "FEAT-005 is first"
as an ordering fact; on the elaboration seam it is not even on the path.

## 4. The recorded decision this pulls against, and the honest way to move it

`ARCHITECTURE.md` "Simulation execution strategy" (#221, recorded 2026-07-26) says the
event-queue interpreter is JLS's **sole** strategy, that a levelized compiled pass is
premature, and names the revisit trigger: "a concrete CPU-scale design on the `riscv/`
trajectory that is unusably slow interactively." `lf-02` § 1.4 measures 1,159 cycles/s on
`bench_kernel.py 2000`. **The trigger has fired.** #370 and #362 together are the
compiled-evaluation programme arriving without saying so — the same plane arrays, the same
dense ids, the same 4.32 ns/node figure — but filed as a memory optimisation and a constant-
factor optimisation, so neither one has to amend the recorded decision or satisfy its
binding equivalence criterion (bit-for-bit agreement with the #202 RV32I golden). That is
the deepest misalignment here. The right move is to reopen #221 explicitly, with the
measurement, and let the flat layout arrive as what it is.

## The concrete alternative

1. **Decompose the 1,190 B** (heap histogram on `riscv/build/k2000.jls`, the same anchor
   the throughput side uses — the boundary comment is right about this).
2. **Land the free deletions**: clear `loadWires`/`probeMap`/`loadPut` at the end of
   `WireEnd.init`, or hold them in a load-scoped side map. Re-measure. Publish the new
   baseline. This may move K17-1 without any of the rest.
3. **Reopen #221** with `lf-02` § 1.4's number and the equivalence criterion intact.
4. **Build `jls.core.sim.NetlistBuilder`** (lifting `HdlExporter`'s `UnionFind`), with the
   `nodeId ↔ Element` map on day one. It has three consumers already: this, HDL export,
   and P5 formula export.
5. **Make the elaborated `Netlist` the single artifact #362 and #370 both cite.** "One
   contract cited by both" stops being a coordination promise and becomes a type.
6. **FEAT-055 consumes the builder in streaming form.** Its scope already assumes this.

## What survives from the issue as written

Invariant 1 (byte-identical goldens) is the right acceptance floor and should govern the
elaborated pass too. Invariant 3's *veto framing* — capacity never outranks per-edit
responsiveness — is the best sentence in either feature and should follow the work, as the
boundary comment says. The measurement-gate dependency on #335 is real. The refusal to
scope this against a wall-clock test is correct and rare. And Open Question 3 ("do the
direct readers consume the flat state, or a view?") is the issue asking the right question
and then answering it inside its own frame; the answer outside the frame is *neither* —
the direct readers keep the model, and the flat state is derived from it.
