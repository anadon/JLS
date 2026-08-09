# Issue #611: TASK-C487-2: the constraint set leaves JLS as a netclass and rule file — byte-identical to a golden, and every emitted keyword one the target parser actually accepts
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the DRU syntax away and the claim underneath is: **JLS should stop being a
tool whose output is only ever judged by JLS.** Every artifact JLS emits today is
adjudicated by a golden JLS itself wrote (`test/resources/hdl/**`: 37 `.v`, 33
`.vhdl`, 1 `.pcf`), with `iverilog`/`ghdl`/`yosys` armed as a second opinion on
*syntax* only. FEAT-060 introduces the first artifact whose correctness is
decided by a tool with its own opinion about whether the design is acceptable.
That is a genuine step in the project's arc, and this task is the hinge: nothing
downstream (#613's failing-direction DRC, #616's back-annotation) exists until
some bytes exist that KiCad will load.

The arc it strengthens is real and already visible in the tree: `PcfEmitter`
(#213) established "authored intent → tool-specific constraint text → byte
golden"; FEAT-004 (#336) will establish "one net partition, names that survive an
unrelated edit"; #223 established a typed seam catalog with an `hdl.exporter`
point already carrying `jls.hdl.HdlEmitter`. This task is the third member of an
emitter family that has never been named as a family. I endorse the end. Three
things about the route are, I think, cut along the wrong seam.

## 1. The netclass is the unchecked conformance claim the issue thinks it avoided

The issue is admirably careful about impedance: KiCad's `NETCLASS` has no
impedance field, so claiming a controlled-impedance *constraint* would be a
conformance claim no tool checks. Correct. But the remedy it prescribes —
"an annotation plus a resolved track width" on the netclass — commits the same
sin one level down. `m_TrackWidth` is a **routing default the router starts
from**, not a rule the DRC enforces. Route the trace at half that width by hand
and KiCad's DRC says nothing. So under the task's own invariant 5 ("JLS claims no
conformance the external tool does not check"), the netclass track width is
exactly as inert as the impedance field would have been, and the acceptance
criteria do not catch it because AC-2 only asks whether the *keyword* parses.

There is a strictly better route that makes the problem disappear: **emit no
netclass at all, and express the resolved width as a rule constraint in the same
`.kicad_dru` file** — a `(rule …) (constraint track_width (min …) (opt …))` over a
`(condition "A.NetClass == …")` or a direct net-name condition. Then:

- The width becomes *checkable*, so it stops being an annotation and starts being
  intent the external adjudicator honours — which is the whole point of the
  feature.
- JLS emits exactly **one** artifact, with one lifecycle and one golden. The
  netclass, as the issue frames it, does not live in a `.kicad_dru` at all: the
  fields it enumerates (`m_TrackWidth`, `m_diffPairWidth`, `m_diffPairGap`,
  `m_diffPairViaGap`, `m_tuningProfile`) are C++ members of the in-memory
  `NETCLASS`, persisted in the **project** file's `net_settings`. Emitting "a
  netclass" therefore means JLS writing into `.kicad_pro` — a file the student's
  tool owns, that carries settings JLS knows nothing about, and that a re-export
  would clobber. That is a data-loss hazard nowhere in the issue's risk list, and
  it is a second artifact with a second determinism story and a second golden.
- The impedance annotation survives untouched as a comment, and the
  documentation sentence in AC-4 gets *easier* to write honestly, not harder.

This is not a detail. AC-2 as written would pass a file that carries a netclass
whose width nothing enforces, while the issue's headline promise is that the
external tool renders the verdict.

## 2. AC-2 already dragged the container into this task — so run the DRC, don't enumerate keywords

FEAT-060 §2 gives a specific reason for cutting #613 away from #611: the DRC
round trip "is the only scope that cannot be run without an external tool armed
in CI, and folding it in would make a byte-golden review wait on container
plumbing." But AC-2 here then requires that every emitted keyword be "checked
against the pinned external parser version rather than against its
documentation." Those two sentences cannot both hold. Checking against the parser
*is* the container plumbing. The cut leaked.

Given that it leaked, the honest resolution is the simpler one: **acceptance is
"the pinned KiCad loads this rule file without a parse error"** — one
`kicad-cli pcb drc --rules <file>` invocation against a trivial committed board,
exit status inspected, skipped via the shipped `assumeTrue` idiom
(`test/jls/hdl/ToolLocator.java`, which already does cross-platform `PATHEXT`
resolution for exactly this pattern). That replaces an enumerated keyword table
with an executable fact. The enumerated table is the worse artifact for a reason
worth stating plainly: **it is a second copy of KiCad's grammar living in JLS's
test suite**, maintained by hand, and it will drift the moment KiCad adds a
keyword or renames one. `SiConstraintExportGoldenTest` asserting a keyword
allowlist is a golden regenerated for the wrong reason wearing a different hat.

Note the pleasant consequence: with load-legality asserted here, #613 narrows to
what only #613 can do — the *semantic* direction, over-length fails and shortened
passes. The two tasks stop overlapping instead of sharing a half-owned container.

## 3. The seam to cut along is a constraint-emitter family, not a third bespoke printer

As written this task produces `SiConstraintExporter`-shaped code sitting beside
`PcfEmitter`, `VerilogEmitter`, `VhdlEmitter` and (soon) FEAT-042's netlist
writer, each with its own determinism discipline, its own golden idiom, its own
external-acceptance harness, and its own place in the CLI (`-export` chooses by
extension; `-board`/`-pins` is a side artifact; now `-si <file>` is a third
convention). Four one-offs is where a family becomes a maintenance surface.

Concrete alternative: define **one constraint-emitter seam** in `jls.netlist`
(the package #336 creates) — an authored per-net intent record, a renderer
interface per target, and one shared acceptance harness (`ToolLocator` + digest-
pinned container + an "emit, feed to the tool, assert it loads" fixture) that any
renderer registers with. Register it in `docs/extension-points.md` next to
`hdl.exporter`, where `ExtensionPointCatalogTest` will keep the table honest.
Then KiCad DRU is a renderer, gEDA is a renderer, the existing PCF retro-fits as
a renderer, and the still-unbuilt XDC/QSF (#82) and SDC (#93) rows in the
standards corpus each cost a renderer rather than a subsystem. This task is the
right moment to pay that cost — the second instance is where the abstraction is
cheap and the third is where it is already late.

Doing it also discharges FEAT-004's own IC-6 ("the second consumer is real"),
which #336 currently plans to satisfy by retargeting `PcfEmitter`. One mechanism,
two obligations.

## 4. AC-3 is the load-bearing criterion and it is third on the list

"Every net named in the rule file appears in the netlist under the same name" is
the criterion that decides whether this feature is real. A constraint on a net
KiCad never heard of is inert — the issue says so — and inert is
indistinguishable from working until a board is fabricated. Everything else here
(byte determinism, keyword legality) can be green while AC-3 is silently false,
because both artifacts would be internally consistent and mutually irrelevant.

It should be criterion 1, and it should be the first test written. It is also the
criterion with no owner: FEAT-042 doesn't assert it, this task asserts it "across
tasks", and neither will fail if the other's naming convention shifts. The cure is
mechanical, not procedural: **both files must be rendered from the same
`Nets(C)` value**, never from two walks that agree by convention — which is
precisely FEAT-004's invariant 1 and precisely why #336 blocks this. Worth
recording in the issue as *blocked_by*, not `ordering_after`: at HEAD there is no
`src/jls/netlist`, no `.kicad_dru` anywhere, and `HdlExporter:353` still names
nets from `getID()`, documented at `Element.java:21` as "reassigned on every
save." AC-1's determinism claim is unachievable until that changes, so the task
as written cannot start.

## The reframing I am not making, and why

There is a case for saying JLS should not go here at all:
`docs/capability-roadmap/sweep-06-physical-boundary.md:570-576` declares PCB
(#140–#146) out of scope — "this is KiCad's domain and KiCad is excellent at it"
— and rejects IPC-D-356A on a reason that reads uncomfortably close to this
feature: "a bare-board test netlist without a board layout has no consumer."
I do not think that reason transfers. A DRU rule file's consumer is the student's
own board in KiCad, which exists; JLS is emitting *intent about* physical data,
never physical data. That distinction is exactly the sweep's own line 3 ("being a
legitimate front end to somebody else's physical flow"), and `PcfEmitter` is the
shipped precedent. The feature is on the right side of the project's own boundary.
But that boundary is stated in a normative-ish roadmap document and this issue
does not cite it. Someone should write the two sentences into the sweep that say
why constraint export is line 3 and not #140–#146, before a future reader reads
those two documents as contradicting each other.

The horizon worth naming while nobody is looking: JLS is the only tool in this
chain that *simulates*. A student authoring a maximum length by hand is doing
arithmetic JLS could do — it has the edge rate (#486), the propagation model, and
the RV32I design as a live workload. The version of this feature that only JLS
could build is one where the constraint is **derived from the simulated design
and then adjudicated externally**, and the student's job is to argue with the
number rather than invent it. #486 and #490 own the pieces; nothing needs to move
today. But if the vocabulary in #609 is being frozen, freeze it so a derived
value and an authored value are distinguishable in the section, or that future
costs a format change.

## Summary of the reframing

Keep the outcome. Re-cut it as: one `.kicad_dru`, no project-file write, width as
a checkable `track_width` constraint rather than an inert netclass default;
legality asserted by loading the file into the pinned tool rather than by an
allowlist JLS maintains; the emitter built as the first renderer on a shared
constraint-emitter seam over `jls.netlist`; and AC-3 promoted to first and made
true by construction (one partition value, two renderers) instead of by a
cross-artifact test that can only observe the failure after the fact.
