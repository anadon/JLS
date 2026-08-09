# Issue #339: FEAT-021: JLS can declare a bidirectional port — the third direction exists in the IR, every emitter renders it or refuses it by name, and a bidirectional pin reads the net
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## The goal is right; the seam is cut in the wrong place

That JLS must be able to say `inout` is not in question. The repository's own
planning corpus makes it one of the two named revisit triggers for a rejected
standard (`docs/standards-adoption/07-waveform-formats.md:150-151`, mirrored at
`docs/capability-roadmap/README.md:53`), the ceiling on IP-XACT export
(`sweep-01-values-and-logic.md:60`), the blocker behind BSDL boundary cells
(`sweep-06-physical-boundary.md:87`), the reason `uio_oe` has nothing to bind to
on Tiny Tapeout (`README.md:1182`), and the hole carved out of the IEEE 91/91a
conformance claim (`01-iec-ieee-symbols.md:44-46`). Six formats and four
capstones is not an overstatement.

What I dispute is the unit of work. This issue defines the deliverable as a
*pin element plus a third IR enum member plus emitter totality*, and prices it at
2–4 maintainer-weeks. The project's own corpus defines the same capability as
**"an `inout` pin element *and a bidirectional subcircuit boundary*"**
(`sweep-01-values-and-logic.md:354`) and prices it at **4–6 maintainer-weeks**,
stating plainly where the money goes: *"the element is small; the cost is the
subcircuit boundary, `Circuit.finishLoad`, the `-pins`/board path, and the two
sealed-hierarchy edits"* (`README.md:297-302`).

The issue's §1 in-scope list has five clauses and **`SubCircuit` appears in none
of them**. Neither absorbing comment mentions it either: both flag
`Pin`'s `permits` clause as "the trap that bites first" and stop there. The trap
that bites *last* is one line further out:

```java
public abstract sealed class Put
		permits Input, Output {
```
(`src/jls/elem/Put.java:16-17`, pinned by
`test/jls/elem/SealedHierarchyTest.java:75`.)

`SubCircuit` materializes a child circuit's boundary as `Map<Input,InputPin> inmap`
and `Map<OutputPin,Output> outmap` (`src/jls/elem/SubCircuit.java:33,35`), creating
one `Input` per `InputPin` and one `Output` per `OutputPin` (`:232-256`). The
element shape carried forward in the absorbing comments — *"one `Input` put, one
`Output` put, one 1-bit enable input"* — has **no single-put representation**, so
an `InoutPin` inside a subcircuit either presents two or three separate
connection points on the parent's box (which cannot be wired to one bus) or is
simply unhandled at `:169`/`:199`/`:232`/`:452`. Hierarchical bidirectionality is
not merely deferred by this design; it is designed against.

That matters because hierarchy *is* the payoff the corpus names: *"a memory
module with a shared data bus drawn as a subcircuit rather than as two separate
in/out ports — which is how every real SRAM, every microcontroller GPIO, and
every I²C peripheral is packaged"* (`sweep-01:373-377`), and *"the `riscv/` CPU
fixture has **zero** `SubCircuit` elements in 228"* (`README.md:288`). The four
serving capstones (#298 PCB, #302 shuttle, #307 KiCad, #310 import) are *all*
flat top-level-boundary consumers. This issue optimizes for the format checkbox
and drops the classroom capability, while carrying the corpus's headline as its
abstract.

## Reframing 1 (primary): two issues, split along the boundary, not along "representation vs element"

§2 explicitly considers and rejects a representation/element split. I agree with
that rejection and think it rejected the wrong split. The seam that actually has
different owners, different risk and different cost is:

- **FEAT-021a — the flat module boundary can be bidirectional.** `INOUT` in
  `HdlModel.Direction`, the four rendering sites, `NetlistImporter`'s port-scan
  realization, the `.pcf` strings, the compile oracles. Serves all four
  capstones, #320 and #328. Days, not weeks (see Reframing 3).
- **FEAT-021b — the *boundary* is bidirectional.** `Put`'s sealed set or a
  bidirectional put, `SubCircuit`'s third map, `InputPin.initSim`'s tri-state
  half-measure (`src/jls/elem/InputPin.java:163-181`, which the corpus already
  identifies as "a half-measure toward bidirectionality at the boundary"),
  `WireNet.setTriState`'s `TriProp` walk, `Circuit.finishLoad`. This is the 4–6
  week item and the one that must not ship dishonest.

The stated fear behind the single roster — *"two issues would let the element
half be indefinitely deferred while the emitters claim a capability the editor
cannot express"* — is real but is answered by refusal, not by bundling: if 021a
lands alone, the editor still *can* express a bidirectional port (Reframing 3),
so nothing is claimed that cannot be drawn.

## Reframing 2: make the census a table the compiler and a ratchet test own, not a one-time artifact

§5 criterion 2 wants a refusal census "produced by a command, not by inspection",
committed as a table. JLS already has this idiom, four times over:
`HdlPolicyTest.exportPolicyIsTotalOverTheElementRegistry` against
`HdlExporter.EXPORTED` (`src/jls/hdl/HdlExporter.java:421-429`),
`SealedHierarchyTest` against `permits` trees, `ExtensionPointCatalogTest`
cross-checking constants against a documented table *in both directions*
(ARCHITECTURE.md "Extension points"), `SaveTagsTest`, `HelpTopicsTest`.

So: do not hand-write a census. Introduce a `(emitter × direction) →
rendered | refused(reason)` table that each emitter *consults*, with a ratchet
test asserting totality and a doc table cross-checked in both directions. Then
criterion 2 is discharged permanently rather than once, and #315's registry-keyed
totality gate is *served* rather than merely satisfied. This matters because
`INOUT` is not the last member: KiCad's pin-electrical-type vocabulary has ten
(#307), BSDL's cell types are `INPUT/OUTPUT2/OUTPUT3/BIDIR/CONTROL/CONTROLR`
(`sweep-01:56`), IP-XACT adds `phantom` (`sweep-01:60`), and FEAT-027 will want
open-drain. The plan as written pays "one enum member + four switch conversions +
one element class + registry/palette/help/icon/dialog rows" per vocabulary item —
ARCHITECTURE.md's own honest sixteen-step list. That is a combinatorial dead end;
the third member is exactly the right moment to stop paying it.

Corollary I would carry into 021b: the corpus offers *"a new `BidirPin` element
**(or a direction attribute on `Pin`)**"* (`sweep-01:355-356`). I do not propose
the attribute form now — `instanceof InputPin` is pervasive and the save-tag
migration is real — but the class-per-port-kind route must be recorded as
capped at three, or the fourth kind arrives and there is no argument left.

## Reframing 3: the flat capability needs no new element at all

This is the alternative the issue never considers, and it makes most of §1
disappear. A bidirectional port is **already drawable in JLS today**:

- `TriState.react` drives its data input when its 1-bit control is non-zero and
  drives `null` (HiZ) otherwise (`docs/simulation-semantics.md` §9).
- `WireNet.propagate` resolves the net and delivers the resolved value to every
  attached `Input` (`src/jls/elem/WireNet.java:443-485`).
- `OutputPin` has an `Input` put, so **an `OutputPin` on a tri-state net already
  reads the resolved net value, not what this circuit last drove.**

So `{InputPin data_out, InputPin oe, OutputPin data_in} + TriState` on one net is
a bidirectional port with correct semantics at HEAD. What is missing is not
behavior — it is **the ability to say that those pins are one port.** Give `Pin`
a `port` string attribute through the existing `Attribute` registry (#52;
`Circuit`'s used-name list at `:1613-1635` keeps pin names unique, so pairing
must be explicit rather than by shared name), fuse the group into a single
`HdlModel.Port` with `Direction.INOUT` in `HdlExporter.buildModel`'s port walk,
and realize an imported `inout` module port as the same group.

What that buys, relative to the plan as written: no sealed-hierarchy edit, no
`SealedHierarchyTest` change, no palette/icon/help-topic/dialog/renderer/
`SaveTags`/`ElementRegistry` rows, no format-version question, no new conflict
semantics, and — decisively — **the load-bearing acceptance test
`aDisabledInoutPinReadsWhatAnotherElementDrives()` is provable today, with
elements that already exist.** Open Question 1 (what does the pin read back
before the resolution fold?) dissolves: the reader is an `OutputPin` and §9
already answers it. Open Question 3 (per-instance enable vs structural) dissolves
into its own recommended answer: enable is structural, supplied by `TriState`,
and the absorbed comment's "Open Question 1 blocks execution" ceases to block
anything. Threat T2 gains its mirror, which is the sharper risk: the proposed
three-connection-point element is not a pin, it is *a `TriState` with a label* —
and the project already ships that.

The honest cost of Reframing 3 is implicitness: a reader must inspect the drawing
to know the module interface. The durable answer is not a pin class either — it
is an explicit circuit-level **interface declaration** (an ordered list of
name/width/direction, owned by `Circuit`, bound to drawn elements), which is also
exactly what #328's fixed shuttle wrapper signature and #360's black-box
component need and neither can get from "whatever pins happen to be drawn". If a
larger goal is visible here, that is it, and it is worth naming before the third
pin class is committed.

## Two smaller corrections

**§5 criterion 4 is close to vacuous at HEAD.** PCF has no direction field:
`Board.Format` has exactly one member, `PCF` (`src/jls/hdl/board/Board.java:35-41`),
and both direction strings land in a `#` comment (`PcfEmitter.java:102-103`,
matching `test/resources/hdl/board/blinky_icestick.pcf`) and in an error message
(`:190-195`). "The board path carries the direction" therefore reduces to "the
comment reads `# inout`". Two of the four celebrated fall-through sites are
cosmetic, which also softens threat T1 considerably. State criterion 4 as "PCF is
enumerated in the capability table as a format that cannot express direction" —
free under Reframing 2 — and defer the real board criterion to a format that has
a direction field, behind #264.

**Naming.** The corpus says `BidirPin` (`README.md:256`, `sweep-06:201` says
`InOutPin`), the absorbing comments say `InoutPin`. Three spellings for one
element in one program; `docs/component-naming.md` exists and should decide once.

## What I would do

Land Reframing 3 as a sub-week change unblocking #298/#302/#307/#310/#320/#328
now; build Reframing 2's capability table in the same change so the vocabulary
can grow without another census; and re-file the drawn element together with the
subcircuit boundary as one honestly-priced 4–6 week feature that matches the
roadmap's own number. I am explicitly disregarding §1 clause 2 and the §5
integration criteria that depend on a new element type, because the capability
they are proxies for is reachable without one — and because as scoped, this issue
can pass its entire Definition of Done while a student still cannot draw a memory
with a shared data bus as a subcircuit, which is the thing the abstract is really
promising.
