# Issue #460: TASK-0089: a drawn circuit leaves JLS as a KiCad netlist whose every component carries a footprint — or leaves as nothing, with every problem named
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the twelve predictions and the LaTeX and one sentence remains: *a student who
drew a working circuit should be able to start a board instead of drawing it a second
time.* That end is right, it is in the arc, and `docs/capability-roadmap/sweep-06-physical-boundary.md:60-64`
already ratifies the ground it stands on — "being a legitimate front end to somebody
else's physical flow" is explicitly *in* scope, unlike computing physical data, which
is not. So the goal survives review intact.

Almost everything between the goal and the acceptance criteria does not.

## 1. The whole task is downstream of a decision that is currently unowned

`#366` §Open Questions 1 says, in its own words, that the schematic-vs-netlist route
decision "**Blocks funding TASK-0089 and TASK-0090**," and delegates the decision to
CAP-13 (#307). #307's recommended default is unambiguous: **"the schematic. … Fund the
`.net` emitter second, over the same partition, once package data exists."** #460 itself
concedes this ("This task is second-rate on its own and the ranking says so").

And #307 was **closed as `duplicate` on 2026-08-03T23:27** — hours after #460 was filed.
So `#366`'s completion criterion "Open Question 1 resolved by CAP-13 (#307)" now points
at a closed issue, and the funding gate on this task has no owner. Filing a 12-prediction
task under a gate whose adjudicator has just been dissolved is how the dominated route
gets built by default. That is a sequencing defect, not a content defect, and it is the
first thing to fix.

## 2. The reframing: emit `.kicad_sch`, not `.net` — and O2 evaporates

The issue treats the footprint trap (O2) as a load-bearing fact of nature. It is not.
It is a **consequence of choosing the netlist as the carrier**, and it is the only
reason `#400` is a hard blocker, the only reason the artifact can be "byte-valid and
functionally empty," and the only reason JLS is being pushed toward owning a part
library with per-part attribution and license notices (`#366` §4 invariant 5) — a
burden #307's OQ2 answers with "**never**."

The corpus's escape hatch is gEDA `.sch` with embedded symbols (TASK-0090). That is
better than the netlist, but it is a legacy format frozen ~2007, upstream dead, reached
only through KiCad's *importer* — a compatibility surface, not a contract, and #366 OQ2
concedes the embedded-symbol premise "has never been run."

**The route the corpus appears never to have considered: KiCad's own native
`.kicad_sch`.** It is an s-expression format, documented, stable since v6, and —
decisively — **self-contained by construction**: every schematic carries its symbols
inline in a `(lib_symbols …)` block precisely so files do not depend on installed
libraries. That is the same property the gEDA route was chosen for, obtained from the
*native, actively-maintained* format instead of a dead one behind an importer.

What that single substitution does to this issue's dependency graph:

- **O2/H2 disappear.** In `.kicad_sch` the footprint is an optional
  `(property "Footprint" …)`. A footprint-less schematic is not an empty board; it is
  a normal schematic that the student completes with KiCad's own *Assign Footprints*
  tool — the workflow every KiCad user already knows. The catastrophic failure mode
  this entire issue is organized around stops existing.
- **#400 (part library) demotes from hard blocker to optional enrichment.** Footprints
  become a value-add, not a precondition. That also relieves the licensing exposure
  #307 OQ2 wants relieved.
- **#394 (PackPlan / refdes) demotes.** The issue argues at length (Stage 2) that
  one-ref-per-drawn-element is "a schematic-path fallback and is wrong here." On the
  schematic path it is not a fallback — it is correct. Packing four drawn NANDs into
  one physical U1 is a *board* concern KiCad's own annotation and packing handle.
- **#427 (width decomposition) demotes.** A schematic carries buses natively; the
  "emitter can describe 1-bit designs only" failure is again netlist-specific.
- **O3/P6/TASK-0049 disappear.** KiCad pin electrical types include `bidirectional`
  and `tri_state` outright. `TriState` renders honestly with no widening of anything.

Four blockers — one of them (TASK-0007) **not even filed** — collapse to one soft
dependency. That is what a reframing that makes the problem disappear looks like, and
it is available for the cost of choosing a different file extension.

## 3. The seam is cut in the wrong place: the printer must not know what a gate is

H4/P11 mandate "an exhaustive `switch` over the sealed `LogicElement` hierarchy with
no `default` arm," and §14 makes it a Definition-of-Done item verified by reading the
diff. **This is the wrong obligation in the wrong package.**

The emitter's declared inputs are `PhysicalNetlist` (refs, package pins, nets),
`PackPlan` (ref/section assignment) and `PartLibrary` (footprints). Those three values
are *already* the result of deciding, per element type, how it becomes physical. A
printer over `(refs × footprints × nets)` has no residual element-kind decision to make.
Reaching back into `jls.elem` to switch on `LogicElement` re-opens a question its inputs
have already closed — and `#366` §4 invariant 4 already assigns that totality obligation
to the disposition table in the binding layer, with a registry-enumerating test. P11
duplicates that invariant one layer too late, where it can only disagree with it.

The elegant version, and the one that pays compound interest:

> `jls.pcb.KicadNetlistEmitter` imports **nothing** from `jls.elem`. It is a pure
> renderer of `jls.pkg.PhysicalNetlist`. The architecture rule added in the same
> commit is not "jls.pcb is headless" but "**jls.pcb depends on no `jls.elem` class**"
> — cheaper to write than the no-`default` review criterion, machine-checked rather
> than diff-read, and strictly stronger.

Cut there and the printer is ~200 lines (`PcfEmitter.java` is 199), and the *same*
`PhysicalNetlist` seam feeds the gEDA emitter, a SPICE `.subckt` printer, and the
IPC-D-356A printer that `sweep-06-physical-boundary.md:570-575` already identifies as
"would be a printer." One data model, N thin renderers. That is the shape; the issue
instead proposes one thick renderer that reaches through its own inputs.

## 4. Two internal contradictions the reframing exposes

- **P6 refuses a design for a field the emitter does not emit.** The minimum record set
  in §7.1 is `(node (ref …) (pin …))` — there is no direction field anywhere in the
  emitted text. Yet P6 aborts any design containing a `TriState`, citing
  `HdlModel.Direction`'s two cases. O4 says in bold "**do not route this through
  `HdlModel`**." Both cannot be true. An emitter that does not use `HdlModel` and does
  not emit direction has no reason to inherit `HdlModel`'s enum as a blocking
  constraint. As written, this refuses essentially every non-trivial student design
  — buses are not exotic — for a reason invisible in the output.
- **P9 celebrates describing `Memory`/`RegisterFile`/`SubCircuit` on a board** while
  those same elements are hard-rejected on the HDL path. Board-describing a `Memory`
  requires binding it to a real memory part, which is exactly #400's hardest content.
  The issue claims the easy half of a capability whose hard half it has deferred.

## 5. Portfolio: this is not the highest-leverage move at the physical boundary

`sweep-06-physical-boundary.md:19-33` measures the flagship: the repository's own RV32I
CPU — trajectory #1 in `docs/grand-architecture.md:52-55`, pinned by
`RiscvCpuGoldenTest`, fuzzed against a reference emulator — **cannot be exported at
all**. `HdlExporter` rejects `Memory`, `SubCircuit`, `ShiftRegister`; there is no
hierarchy. The sweep names change **A (total export coverage + hierarchy)** as the
prerequisite for essentially the whole descent, and observes the teaching inversion in
the tree today: a student who structures a design *well* is punished by the exporter.

`grand-architecture.md` §2 names three funded trajectories — CPU teaching, FPGA
bridge, collaborative editing. The board path is not among them. Meanwhile change A
transitively unblocks #110, #109, #111, #89, #82, #215 and #304. Against that, a KiCad
netlist emitter gated behind four tasks (one unfiled) to reach a route its own capstone
ranked second is a poor allocation of the next 1.5 weeks.

## What I am disregarding, explicitly

I am setting aside **P6** (TriState refusal — refuses for an unemitted field), **P11
and its DoD line** (the no-`default` `LogicElement` switch — wrong layer, duplicates
`#366` invariant 4), and the framing of **#400/#394/#427 as hard blockers** (they are
blockers of the netlist carrier, not of the goal). I keep, without reservation, the
all-or-nothing contract (P5), the emit-returns-text/caller-writes split (§7.8), byte
determinism (P10), the golden regime, and the `-parts`-is-#400's discipline (P12) —
those are the parts of this issue that encode real, hard-won project taste.

## Concrete recommendation

1. Spend the afternoon #366 OQ2 asks for, but spend it on `.kicad_sch`, not gEDA
   `.sch`: hand-write a ten-line native schematic with one inline `lib_symbols` entry
   and no footprint property, open it in KiCad 10. If it opens — and the format's
   self-contained design says it will — §2 above becomes the plan.
2. Re-home the route decision orphaned by #307's closure onto #298 or #366 itself, and
   record it, before either emitter is funded.
3. Re-scope this issue to: *a ~200-line element-agnostic renderer of
   `jls.pkg.PhysicalNetlist`, guarded by an architecture rule that it imports no
   `jls.elem` class*, scheduled **after** the schematic path, with #400 as an
   enricher rather than a gate.
4. If a week of physical-boundary work is available before any of that, spend it on
   `HdlExporter` hierarchy + `Memory` + `ShiftRegister`. It unblocks the flagship
   design, the FPGA trajectory, and — transitively — this issue too.
