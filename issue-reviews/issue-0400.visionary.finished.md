# Issue #400: TASK-0085: a versioned schema says what a chip is and a drawn element can say which chip it becomes — data on the classpath, no geometry, no Java to extend it
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the schema talk away and the goal is one sentence: **a student should be able
to take a circuit they drew and build it — on a breadboard, or on a board a fab will
make.** That is a real, unserved capability. `docs/grand-architecture.md` §2 names
exactly three latent products (CPU teaching tool, FPGA bridge, collaborative editor);
the physical/through-hole path is in *none* of them, and the issue is honest about
that ("no open issue touches the physical programme at all"). I do not treat that
absence as a reason to decline. JLS already carries a design to a *physical* target
once — `jls.hdl.board` + `PcfEmitter` + `PinBindings`, shipped under #213/#264 — so
the trajectory is established, and the through-hole case is its sibling, not a new
tool class.

My objection is not to the destination. It is that this task cuts the seam in the
wrong place, in the wrong order, and builds a mechanism the project has already
decided it wants exactly one of.

## Finding 1 — this is a third data-library mechanism in a project whose recorded direction is to have one

`docs/grand-architecture.md` §3 names `ElementRegistry`/`ElementType` "the *seed* of
the plugin mechanism" and states the determination: **generalize that registry**, do
not grow siblings beside it. `docs/extension-points.md:30` already types the seam
(`elem.element-provider` → `jls.elem.ElementType`). And `docs/capability-roadmap/README.md`
§P6 spells out the generalization in the very words this issue needs:

> **Cells as data.** Extend `ElementType` … so a cell can be a record (name, pin
> list, logic function, timing figure, area, capacitance) **loaded from a library
> file** rather than compiled in.
> **A shuttle target.** Generalize `jls.hdl.board` from `(name, fpga, format, pin map)`
> … to a target descriptor that can carry a wrapper template.

TASK-0085 does neither. It creates `src/jls/pkg/` with a new `PartPackage` record, a
new bespoke `.parts` grammar, a new `SCHEMA` version axis, a new `-parts` flag, a new
override/extend resolution order, and a new registry-keyed policy table — all
structurally parallel to the cell library P6 says to build, and all disjoint from it.
A `74LS08` section and a `sky130_fd_sc_hd__nand2_1` are *the same abstraction*: a
named realization of a logic function, with pins, a footprint/abstract view, timing
and loading figures, and provenance. Ship `jls.pkg` as written and JLS acquires two
libraries, two grammars, two version headers, two resolution orders, two curation
obligations, and a merge nobody has budgeted. The issue's own O2 warning
(`Boards.ALL = List.of(ICESTICK)` "is the precedent *and* the warning") is read too
narrowly: the lesson of `Boards` is not "load tables from a file", it is "there
should be one table mechanism, and it should be the registry".

## Finding 2 — the binding does not belong in the `.jls` file, and the project already shipped the counter-example

§7.1(b) and §7.7 add optional per-element saved state (part number, section index,
part value) keyed by `Element.getStableId()`, plus a migration story, plus an
architecture rule that the library must *not* live in the circuit. But the project's
own physical-binding precedent — the one this issue cites three times as the idiom to
copy — does the opposite: `-pins <file>` (`JLSStart.java:785`) reads a **side-car**
`PinBindings` file, and `#213`'s design binds a drawing to a physical target with
**zero** change to the save format.

That is the better seam, and for a reason bigger than tidiness. A package assignment
is a property of a *build*, not of a drawing: same circuit, 74LS in the teaching lab
and 74HC on the take-home board; same circuit, breadboard sections one term and a PCB
the next; an instructor handing out one `.jls` and three realization files. Put the
binding in the element and you have welded a build decision into the artifact students
share, widened the format for it, and made "re-target this design" a file edit instead
of a flag. Put it in a `-realize <file>` side-car and §7.7's migration paragraph, the
optional-state rule, and the "historical files load unchanged" criterion all
*disappear* — the problem is gone rather than handled. The `.jls` format stays exactly
as it is.

## Finding 3 — the realization policy quantifies over the wrong domain

`PartLibrary.forElement(ElementType, int bits)` cannot express the mapping it exists
for. `Gate` carries **two** saved attributes, `bits` and `numInputs`
(`src/jls/elem/Gate.java:266,278`). A 2-input AND is a `74LS08`, a 3-input AND is a
`74LS11`, a 4-input AND is a `74LS21` — same `ElementType`, same `bits`, three
different packages. And an 8-bit 2-input AND is *eight* gates, i.e. two `74LS08`
packages plus a section of a third: the honest return type is a multiset of sections,
not one `PartPackage`. The realization relation is `(function, arity, width) →
multiset⟨Section⟩`; the issue keys it on `(ElementType, bits) → PartPackage`.

The totality ritual makes this worse, not better. P2 demands a disjoint partition of
all 35 registry types. But `Text`, `JumpStart`, `JumpEnd`, `WireEnd`, `Stop`, `Pause`,
`Display`, `SigGen`, `TestGen`, `Splitter`, `Binder`, `Constant`, `InputPin`,
`OutputPin`, `SubCircuit` — over half the registry — are not chips under any reading.
The test that results is ~20 rows of `NO_DEFAULT_REALIZATION` prose and ~15 rows that
are *wrong in the way above*, and it will pass, and it will read as coverage. The
`HdlExporter` shape being copied works because there the domain genuinely is the
class: an `AndGate` renders as `&` regardless of arity. Realization is not that.
(Separately: `classifiedElementClasses()` and the `REJECTED` bucket cited as O4 do
**not exist on `master`** — see the issue's own #493 evidence-pin comment and my grep;
`master` has three `Set`s and no accessor. The shape this task says it copies is
itself unlanded work owned by #492.)

## Finding 4 — a schema whose every falsifier is deferred, and dead columns by design

#349 §2 rejected a one-task cut because "separating [schema and binding] produces a
schema with no consumer" — and then cut TASK-0085/TASK-0055 in a way that produces
exactly that. The first real consumer (packing) is a whole feature away (#365); the
emitters are another (#366). H1 (do real parts' sections share pins?) is answered by
*data*, which is TASK-0055 and unfiled. H2 (is a footprint name sufficient for
`pcbnew`?) is answered by *an emitter*, which is #366. So the task ships the artifact
whose defects are, in the issue's own words, "the expensive failure" — and defers every
experiment that could detect them. Meanwhile §7.12/P10 deliberately ships electrical
columns with an architecture test **forbidding anyone from reading them** until #341.
Transcribing a column no one may read, and pinning that prohibition with a test, is a
strong signal the column belongs to a later cut: the issue's own P6 refusal machinery
exists precisely so a schema bump is safe, so "add it later" costs one version bump,
not a revisit of entries that do not exist yet.

## The alternative I would build instead

**A. One vertical slice, end to end, before any general schema.** Take `74LS08`
*alone* and a four-gate drawing, and drive it all the way to a KiCad netlist that
`pcbnew` accepts without discarding the component. That single experiment refutes or
confirms H2 by *measurement* rather than by ratifying a recommended default, and it
delivers the capability statement ("a board tool accepts it") that #349 says is the
whole point — in a fraction of 2 maintainer-weeks. The schema then becomes whatever
that slice needed, discovered rather than designed. This deliberately re-cuts across
#349/#365/#366's feature boundaries; the current cut is horizontal (schema, then data,
then packing, then emitters) and puts a permanent design decision furthest from its
feedback. I am explicitly disregarding §14's "no part data is transcribed here
(TASK-0055 owns it)" and §8's "explicitly NOT built here: the emitters": one part and
one emitter path *is* the experiment.

**B. Side-car realization file, not element state.** `-realize <file>` in the shape of
`-pins`, parsed with the same `PinBindings` aggregation idiom the issue already
commits to reusing. Deletes §7.1(b), §7.7(a), the migration criterion, and the
optional-state invariant outright, and makes retargeting a flag.

**C. Seat the library on the registry seam, not beside it.** Whatever the slice proves
necessary, land it as an extension of the `ElementType` descriptor / a new typed point
in `docs/extension-points.md` — the P6 "cells as data" mechanism, arriving early with
a through-hole first customer instead of a silicon one. That is strictly *more*
valuable than the roadmap assumes: P6 is gated on programs A/B/C (export coverage,
reset model, strength lattice) and banded at 20–32 weeks, but the *cells-as-data* half
is gated on none of them, and a 74-series library is a cheaper, safer first tenant
than a Liberty subset. Done this way, TASK-0085 stops being a detour from the
roadmap's most expensive program and becomes its on-ramp.

**D. If a text grammar is still wanted, do not invent one.** `docs/grand-architecture.md`
§9 records "orchestrate external tools, never reimplement" and the standards-adoption
tree exists to prefer adopted formats. A bespoke `.parts` with its own `SCHEMA` axis is
a format JLS owns forever, at bus factor 1. The footprint association that actually
opens `pcbnew` is a KiCad netlist field; the part identity the wider world uses is a
KiCad symbol library. Reading one of those, or emitting into it, is the same work with
someone else's maintenance behind it.

## What I would keep verbatim

The provenance-or-it-does-not-ship rule (§7.4, §7.11) is excellent and should outlive
any re-cut. So should total version refusal (P6), aggregate malformation reporting
(P4/P5), duplicate-key-is-an-error (P5), natural ordering for reproducible BOMs
(P11/O6), and the flat refusal to ship geometry (P9) — that boundary is drawn in
exactly the right place and is the single best judgement in the issue.

## Verdict

**rethink.** The capability is real and worth funding; the artifact is not the right
first move. Build the slice, put the binding in a side-car, and seat the library on
the registry seam the architecture has already chosen — then the schema writes itself
from evidence instead of from five recommended defaults awaiting ratification.
