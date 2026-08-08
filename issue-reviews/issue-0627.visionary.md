# Issue #627: TASK-C523-1: the emitted netlist is re-parsed and proven to be the circuit JLS simulated — a stable-id net-partition isomorphism over the whole fixture corpus
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the KiCad vocabulary away and #627 is asking for a third verification
layer that JLS does not have anywhere. The project already ships two:

1. **byte goldens** — `VerilogExportGoldenTest`, `VhdlExportGoldenTest`,
   `test/jls/hdl/board/PcfGoldenTest`, ~70 files under `test/resources/hdl/`;
2. **structural sanity beside the golden** — `test/jls/hdl/VerilogStructure.java`,
   whose own javadoc states the thesis of #627 four years early: *"This is not a
   Verilog parser … but it catches the failure modes golden diffs alone cannot
   explain."*

Layer 3 is *semantic parity against the source model*: re-read the artifact and
prove it denotes the same circuit. #627 is that layer. The arc it strengthens is
the one #336 opened — one partition, many consumers — by adding the only check
that makes the "many consumers cannot disagree" claim observable from outside
the process. Endorsed as an end. Everything below is about the seam it is cut
along.

## Ground truth at HEAD

Nothing in the pipeline #627 tests exists. `src/jls/netlist` — absent.
`src/jls/pcb` — absent. `grep -riE "footprint|refdes|pinout|kicad" src/` —
no hits. `grep -rn "stableId" src/jls/hdl/` — no hits. `test/fixtures/` holds
four `.jls` files. The HDL "fixture corpus" is not a corpus of files at all: it
is Java, one `HdlCircuitBuilder` call chain and one `@Test` per fixture inside
`VerilogExportGoldenTest`. There is no `@ParameterizedTest`, `@TestFactory` or
`DynamicTest` anywhere under `test/`.

That last fact matters more than it looks. AC-5 — *"a new fixture entering the
corpus is covered without editing the test"* — is not a property of the test
#627 asks for; it is a property of a corpus mechanism that does not exist and
that no filed issue owns. As written, AC-5 silently smuggles in a
fixtures-as-data conversion of the whole HDL suite.

## Reframing 1 — cut the seam at "artifact parity", not at "KiCad netlist"

#627's own justification is format-neutral: a byte golden *"cannot distinguish a
structurally wrong netlist from a right one once the golden itself is wrong."*
That sentence is true, today, unchanged, of the 37 `.v` and 33 `.vhdl` goldens
already in the tree. They are pinned by bytes and by a regex-based sanity pass
that leans on the emitter's own formatting. Nothing pins that the emitted
module's net structure equals the circuit's.

So build one harness, not one test class:

```java
interface PartitionReadback {          // headless, jls.netlist-adjacent
    NetPartition induce(byte[] artifact);   // artifact terminals -> nets
    TerminalKey keyOf(SourceTerminal t, Binding b);  // source -> artifact key
}
```

with instances for KiCad `.net` (`(refdes, pin)` via the PackPlan), Verilog
(`(instance, port, bit)`), gEDA `.sch` (TASK-0090), Yosys JSON (#321), SPICE
later — and one `@ParameterizedTest` over (fixture × readback). `junit-jupiter`
6.1.2 is already in `pom.xml` and carries `junit-jupiter-params`; no new
dependency.

The marginal cost over #627-as-written is one interface and a parameterization.
The payoff is that TASK-0090, #321 FEAT-019 and any future emitter inherit
parity instead of each filing its own C523-shaped row later. Cutting the seam
per-format is how JLS ends up with five partition walks again, one verification
layer down.

## Reframing 2 — the parser belongs in `src/`, not in `test/`

#523 records plainly that the readback direction (#307 claim 3) *"has no owning
issue"*. #627 forces someone to write a KiCad `.net` parser and then confines it
to test scope, where it is deleted from the project's capability surface the
moment it compiles.

Put it in `src/jls/pcb/KicadNetlistReader` instead. Three things fall out at
near-zero marginal cost:

- it is the seed of the reader half #523 says nobody owns;
- it is a **non-emitter in-tree consumer of the partition type** — exactly what
  #336's IC-6 and its Open Question 4 are still shopping for, and a better
  answer than retargeting `PcfEmitter`, because it exercises the partition in
  the inbound direction;
- AC-2 stops being a discipline and becomes a compile-time fact. A reader whose
  only input is `byte[]` cannot reach emitter state. #627 proposes *"a test
  asserts the test's own shape"* to guard this; a package boundary guards it
  better and needs no meta-test.

The honest cost: production code must handle hostile input, per `SECURITY.md`
and the `UntrustedFileHardeningTest` precedent. Scope the reader to "parse what
JLS emits; refuse everything else with a named `LoadError`-shaped diagnostic."
That is a truthful contract and still a foundation.

## Reframing 3 — say what the isomorphism actually proves

Both sides of the comparison are keyed through the *same* PackPlan binding the
emitter used. A binding that is a consistent permutation of pin numbers — the
`pin` function #366 §3 singles out as *"the silent-when-wrong one"* — passes
this isomorphism and yields a wrong board. #627 nowhere cites #366's evidence
criterion 4 (pin order against a published pinout), which is the only check that
catches it.

The theorem is therefore weaker than the title: `emit` is a **partition
homomorphism relative to a binding β**. That is genuinely valuable — it catches
merged nets, dropped nets, lost cascade nets, escaping and name-collision bugs.
It is not "the circuit I simulated", because β is unproven. Two consequences:

- state it that way in the emitted artifact's header and in the release note, so
  AC-4's narrowing discipline narrows the *right* claim;
- make #366 criterion 4 a co-requisite of #627, not a distant sibling. CAP-05
  shipping a green "provably the circuit I simulated" beside a permuted pinout
  is the worst outcome this feature can produce.

There is also a construction-vs-test point worth stating. TASK-0089's §7.10
makes `emit` a fold over `PhysicalNetlist`: one `net` record per net, by
construction. In that shape the emit-direction isomorphism is *true by
construction* and the test proves little about today's emitter. What is
genuinely at risk is serialization (escaping, name collision, `code` numbering)
and β. Re-aiming at those two is smaller, sharper, and finds more.

## Reframing 4 — generate the corpus, do not curate it

AC-5's wish is right and its mechanism is wrong. Eight hand-built fixtures are
weak evidence for a structural property over an unbounded input space; the
interesting cases are precisely the ones nobody thinks to draw — same-named
jumps bridging three nets, a multi-driver tri-state net, a wide bus that
cascades across slices, four gates sharing one refdes, a net whose only put is
an `Output`.

A deterministic-seed generator over `HdlCircuitBuilder` plus a shrinker makes
AC-5 vacuous: there is no corpus to add to. No property library exists in
`pom.xml` and adding one is a real decision, but even a hand-rolled generator
with a recorded seed beats a fixed eight. If the curated corpus is kept, then
the fixtures-as-data conversion AC-5 assumes must be filed as its own row — it
is a change to `VerilogExportGoldenTest`'s whole regime, not a detail of this
task.

## Reframing 5 — the sequencing is upside down, and the fix is free

`ordering_after: [336, 365, 366, 460, 468]`. Five unbuilt features, of which
#336's own children (#468, #373) are not landed and #366's Open Question 1 (the
schematic-vs-netlist route) is not answered. This 1.5–2 mw of verification is
spent last, on code that does not exist, and its harness design is validated for
the first time against a corpus that does not exist either.

The cheapest possible version of this check is available **today**, at zero
prerequisite cost, in the Verilog direction: parse the emitted `.v`, induce a
partition over `(instance, port, bit)`, compare it to `Circuit`'s `WireNet`s.
That instance:

- runs against 37 real goldens and shipping production code, so it can find real
  defects rather than defending unbuilt ones;
- proves out the readback SPI, the keying abstraction and the corpus mechanism
  while they are still cheap to change;
- reduces #627's KiCad instance to "write one `PartitionReadback`" once #366
  lands, cutting its band.

This is the single highest-value change I would make. Compare #366 §6's own
instinct — *"the single highest-value hour in this feature is not on the
critical path"* — the same reasoning applies here and points at the same move.

## What I am disregarding, and why

- **AC-1's `NetPartitionIsomorphismTest` as a KiCad-specific class.** Replace
  with a parameterized parity harness over a `PartitionReadback` SPI; KiCad is
  its first instance, Verilog its zeroth.
- **AC-5 as written.** The corpus mechanism it presumes does not exist. Either
  generate the corpus (preferred) or file the fixtures-as-data conversion.
- **AC-2's meta-test** ("a test asserts the test's own shape"). Sound instinct,
  wrong mechanism: move the reader into `src/` and the property holds by
  construction. Keep the ArchUnit-style rule from #468 that forbids a second
  walk; that one is load-bearing.
- **Nothing about AC-3 or AC-4.** AC-3's refusal to weaken the isomorphism to an
  approximate match is the best sentence in the issue and should survive any
  reframing verbatim. AC-4's narrowing discipline is right, and Reframing 3
  makes it narrow the claim that is actually proven.

## Verdict

Endorse the end; change the seam and the order. The parity layer is real, it is
missing project-wide rather than KiCad-specific, and the version of it that
lands first should be the one that can run this week.
