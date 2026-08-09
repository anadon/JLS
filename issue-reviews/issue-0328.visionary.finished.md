# Issue #328: FEAT-044: a design drawn in JLS is wrapped in a shuttle's fixed top-level signature and carried along a submission path that has actually been walked
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the apparatus and the goal is one sentence from `docs/capability-roadmap/README.md:906`:
*"Map your adder to sky130 and count the cells. Fit your design in a tile."* The
roadmap's own P6 section is unusually honest about where the value sits — not in the
chip, but in **the budget**: 8 dedicated inputs, 8 outputs, 8 bidirectionals, one
clock, one reset, one tile of area. *"Every one is a constraint students must design
against, and constraints are where digital design is actually taught."* The physical
chip is the emotional payload; the budget is the pedagogical one.

FEAT-044 as written optimizes for the emotional payload and treats the pedagogical one
as a side effect. That inversion is the source of every reframing below. It is also why
I endorse the capability without endorsing the acceptance criteria.

The trajectory itself is sound and well-attested: `docs/capability-roadmap/sweep-06-physical-boundary.md:362-405`
(item F), README §P6, the #304 verdict at README:1152-1190. The claim "JLS should reach
silicon" is not speculative here — it is a costed, adjudicated roadmap item with a
named surviving vendor path and a fetch-verified fixed signature. The mechanism
(`PcfEmitter` + `PinBindings` + all-or-nothing refusal, `src/jls/hdl/board/`) really
does transfer. I have no quarrel with the destination.

## Reframing 1 — the acceptance gate is a calendar event masquerading as a test

Criterion 4 ("commands each of which has been run") and criterion 5 ("no `_TBD_` cell,
or the check is red") make an **unrepeatable, externally-owned, months-long event** the
gate on a software feature. §6 admits this outright: *"the shuttle window is a calendar
dependency"*; §7 already pre-writes the REPLAN for a closed window; open question 3
invents a "window closed" outcome cell so criterion 5 stays satisfiable *without lying*.
Three separate escape hatches for one criterion is the design telling you something.

The deeper problem is half-life. A single walked submission is a snapshot that begins
decaying the moment it is recorded — and this project has the receipts:
`sweep-06:686` records Efabless shutting down in March 2025 and taking chipIgnite with
it, and `README:1159` records OpenLane 2 becoming LibreLane. A one-shot walk record
against a vendor that has already died once is weaker evidence than a **mechanical
check that re-runs**.

**The alternative that dissolves it.** Tiny Tapeout's template repository *is itself a
CI harness*: it ships a fixed `tt_um_*` shell, a cocotb bench, and a GDS workflow that
runs LibreLane. Make the acceptance criterion "**JLS's generated wrapper drops into a
digest-pinned clone of the template and the template's own CI goes green**", exercised
in FEAT-023's toolchain lane on every push. That is repeatable, calendar-free, catches
upstream drift as a *failing build* rather than as criterion 6's separate digest test,
and satisfies the project's own honesty rule (`README:1186`) — *"emits shuttle-shaped
artifacts accepted by [named tool version]"* — better than a submission receipt does,
because it names the version and re-checks it. The actual walk then becomes what it
should be: a CHANGELOG entry and a photo, recorded when the window opens, gating
nothing.

I am explicitly disregarding acceptance criteria 4 and 5 as stated. Criterion 5's
motivating evidence — the six `_TBD_` cells at `docs/icestick-bitstream-handoff.md:119`,
which I verified are still there — is real and the diagnosis is right. But the cure for
"we shipped a document nobody executed" is *an executable document*, not *a stricter
promise about a document*. `scripts/icestick-handoff-selftest.sh` is the shape that
already works in this tree; extend it, don't add a placeholder linter.

## Reframing 2 — `Board.Format` is the wrong seam, and invariant 3 defends it

Open question 2 asks "`Boards` row or separate target?" and recommends (a). I think the
question is posed one level too low, and the recommended answer entrenches an
abstraction that is about to break.

`Board.Format` is *one file with one extension*. `src/jls/JLSStart.java:473` is the
proof: the constraint filename is literally `basename + "." + board.format().extension()`.
A shuttle target emits **three artifacts of three different kinds** — a Verilog module,
an `info.yaml`, and a flow config — which is not a filename extension, it is a
directory. Making `TT` a `Format` constant means either lying about `extension()` or
special-casing the one constant that isn't a file suffix.

Meanwhile invariant 3 ("no `default` arm; exhaustiveness is the compile-time gate") is
correct discipline aimed at the wrong enum. It hardens a dispatch that the shuttle work
is the first thing to outgrow.

**Alternative design.** Cut at `Target`, not `Format`:

```
interface Target { String name(); List<Artifact> emit(HdlModel, Bindings); }
record Artifact(String path, String text);
```

`Board` becomes a `Target` whose `emit` returns a single PCF artifact — a mechanical,
golden-preserving refactor (invariant 6 holds; `blinky_icestick.pcf` stays byte-identical).
The shuttle becomes a second `Target` returning three. All-or-nothing survives intact and
gets *stronger*: the whole `List<Artifact>` is built in memory, then written, so the
"partial submission directory" §2 rightly fears is structurally impossible rather than
merely forbidden. `PinBindings` transfers unchanged; the shuttle's "pins" (`ui_in[0]`…)
are just a fixed pin map. Exhaustiveness moves from a switch to the type system, which is
a better version of what invariant 3 wants.

This also fixes an asymmetry the issue never names: on a board the user *chooses* which
pad; on the shuttle the pad set is fixed and the user chooses which of 8 slots. Same
mechanism, but the error messages and the docs are different, and a `Target` seam lets
them differ without a conditional.

## Reframing 3 — the highest-value slice ships years earlier and needs no shuttle

The budget is enforceable *today*, against the model `HdlExporter.buildModel` already
produces, with zero external dependencies:

> `jls -target tt --check design.jls`
> → "17 input bits declared, 8 available. 3 output bits over. No reset on `count_q`
> (ASIC synthesis discards `= 4'h0`). Memory `RAM1` is not exportable."

That is a **fit report**. It is unblocked by #339, #327 and #359 — it doesn't emit a
wrapper, it evaluates `ok(β)` and prints the answer. It delivers the roadmap's stated
pedagogy ("fit your design in a tile") on the day it lands, works for a course whose
budget for a real tapeout is zero, and doubles as the diagnostic front end for the
wrapper when the prerequisites do arrive. Every dollar of §3's binding math is spent
building it; the wrapper emitter is then string interpolation over an already-validated
binding.

If I could file one issue in place of this one, it would be that. FEAT-044 would then
become genuinely small — which is what its 2–4 week estimate has always implied and its
§6 gating has always contradicted.

## The dependency the graph is missing

`blocked_by: [327, 339, 359]` carries P2's reset and bidir gates and the toolchain gate.
It does **not** carry the roadmap's *first* named gate (`README:1194`): *"Without P3's
export coverage, the only designs that can be taped out are gate toys."*

That gate is real and verifiable at HEAD. `src/jls/hdl/HdlExporter.java:88` puts
`SubCircuit` and `Memory` in the **reject** bucket — a hierarchical design is a refusal,
not an export. #358 (FEAT-018, hierarchical export) and #873 (the reject-list closure)
are open and own it. A student's term project — a small CPU, a state machine with a
lookup ROM — is precisely a design made of subcircuits.

So FEAT-044 can land fully green and be unusable by its named audience: everything it
can wrap is a flat gate netlist. #310 (CAP-15) is listed here as a *beneficiary*; for
the hierarchy half, the arrow points the other way. Either add #358 to `blocked_by`, or
state in §1 that first submissions are flat-only and say so in the submission document
— the honest version of the same fact.

## What I would keep unchanged

- **Criterion 6, the digest pin.** The strongest idea in the issue, and its motivating
  evidence (a stale template already cited in the corpus, `sweep-06:92`) is on record.
  Keep it whatever seam you cut at.
- **All-or-nothing generation.** Non-negotiable and already proven by `PcfEmitter`.
- **Invariant 5** ("a returned chip is not a returned test"). Rare intellectual honesty;
  it is also, read carefully, an argument *for* Reframing 1 — if the chip proves nothing,
  the walk should not gate anything.
- **The refusal to own place-and-route, DRC, LVS, GDS.** Correct scope boundary,
  consistent with `docs/icestick-bitstream-handoff.md:94-102`'s honesty note.

## Verdict

**endorse-with-reframing.** The destination is right and well-argued; the route has
three fixable problems. Cut at `Target` rather than `Format`; make acceptance a
reproducible run of the pinned template's own CI rather than a once-walked submission;
and ship the tile-budget fit report first, because it carries the pedagogy the roadmap
says this program is for, and it can carry it now.
