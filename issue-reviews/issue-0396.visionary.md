# Issue #396: TASK-0093: the breadboard is checked against the schematic per discrepancy, and the placed arrangement can drive the simulation
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the apparatus and three distinct things are bundled in one task:

1. **A second derivation of connectivity** from placement, in the same `NetPartition` type as the
   schematic one (H1) — an idea worth having.
2. **Electrical rules over a net partition** — C4 (unconnected pin), C5 (power/ground unconnected),
   C6 (contention). None of these compares two views. They are properties of *one* partition.
3. **A comparison of two partitions** — C1 (split) and C2 (merge), which §7.10 correctly identifies
   as the two directions of `φ` failing to be a bijection, plus C3 (unplaced part), which compares a
   `PackPlan` to a placement and is not a net question at all.

The issue names C6 as "the finding that justifies the whole task" and then, honestly, descopes it by
default because #387 has not landed. That is the tell. The load-bearing value has been delegated to a
prerequisite, and what actually ships under the recommended default is C1–C5: a graph diff between a
drawing and another drawing, plus two rules that never needed the second drawing.

## Where this pulls against the project's arc

`docs/capability-roadmap/sweep-06-physical-boundary.md` is the project's most considered statement
about exactly this boundary — 709 lines enumerating changes A through G with sizings, ripple effects
and papered-over fictions. **The word "breadboard" does not appear in it.** `grep -rn breadboard
docs/ -i` returns exactly one hit, an aside in `standards-adoption/11-costed-rejections.md:498`.
Meanwhile that document names, as change **G**, "electrical rule checking (the honest, small answer)":
undriven inputs, multiply-driven non-tri-state nets, combinational loops, width mismatches, fanout
limits, clock nets on data pins — 3–5 maintainer-weeks, over the element graph, no new view, no file
format change, and "the one thing JLS most conspicuously lacks against Digital and Logisim-evolution."

C4, C5 and C6 *are* change G, re-derived under a breadboard frame and made to wait behind a canvas.
That is the misalignment: a capability the architecture already scoped, sized and justified is being
delivered as a side effect of a programme (FEAT-043 → #297 CAP-04) the architecture documents never
mention. The same document also names change **C** (strength lattice) at 6–10 weeks and 25 touched
`react()` methods; #387 is that. So this 2-week task sits behind ~10 weeks of value-domain work plus
TASK-0092, TASK-0036, TASK-0105, TASK-0007, TASK-0091 and #394 — and its one unique finding is the
part that gets dropped if the 10-week item is late.

A second alignment problem worth naming plainly: the entire audience-fit warrant for this programme is
maintainer ruling D9, quoted from `docs/plan/evidence/BRIEF.md` §13 — a file the issue's own pin
notice (#493) says does not exist on `master` and cannot be re-pinned. `ARCHITECTURE.md` has a
"Recorded decisions" section built precisely for rulings of this weight (i18n non-goal, plugin trust
boundary, simulation execution strategy — each with rationale and a revisit trigger). If the
breadboard programme continues at all, D9 belongs there and sweep-06 needs the physical-bench row it
currently lacks. Otherwise a 9–15-week feature rests on a citation that resolves nowhere.

## Alternative framing 1 — cut the seam at the partition provider, not at a binding enum

§7.4's `PhysicalBinding` is a two-valued switch on the simulator's elaboration input. That is the
narrow version of a genuinely good idea. The general version: **the simulator elaborates over a
`NetPartition` obtained from a named provider**, and `SCHEMATIC` is simply the default provider.
Then:

- The breadboard is *a* provider, not a special case, and `jls/bread/PhysicalBinding.java` stops
  existing.
- `src/jls/hdl/imp/NetlistImporter.java` (Yosys JSON, already in-tree) becomes a provider, so
  "simulate the synthesized netlist against the drawn one" is the same differential machinery.
- A future layout view (sweep-06 change E) and cell-mapped netlists (change D) plug into one seam
  rather than each adding an enum constant.
- #329's Open Question 2 ("which view drives the simulation") stops being a boolean and becomes data.

Crucially, **P6/H3 generalizes with it**: "the observable difference between two runs equals exactly
the set the report names, in both directions" is the same discipline as `ARCHITECTURE.md`'s recorded
equivalence criterion for any future simulation strategy and as `riscv/fuzz_diff.py`. That is the best
thing in this issue and it deserves to be a project-wide invariant on the provider seam, not a
one-off test in `test/jls/bread/`.

## Alternative framing 2 — the breadboard's model is a text wiring list, not a canvas

The issue takes the canvas (TASK-0092, #401) as a hard prerequisite, which drags TASK-0036 (per-view
geometry + op discriminator) and TASK-0105 (per-view palettes) onto the critical path — four tasks and
a rewrite of a sealed op vocabulary before a single discrepancy can be reported.

None of that is required to answer the research question. JLS already has the pattern: `-pins` reads a
plain text side-file of `port pin` lines and applies all-or-nothing binding discipline
(`JLSStart.java:783-786`, #213). Do the same here:

```
jls -b circuit.jls -breadboard board.txt -breadboard-check report.txt
```

where `board.txt` is the physical vocabulary #394's `wiring.net` already establishes — `U3.1 -- U7.11`,
`U3.14 -- VCC`, one edge per line. Every prediction P1–P8 in this issue is satisfiable against that
input. It diffs in version control, it is autograder-native (the repo's actual grading trajectory:
`-t` vectors, VCD, the `ghcr.io/anadon/jls` batch container), a student can produce it from a photo of
their bench build or an instructor can hand it out, and it is headless by construction — which the
issue itself wants, since §7.9 says this task adds no GUI at all.

The canvas then becomes an optional *renderer and editor over that file*, filed on its own merits and
its own schedule, rather than a gate. That deletes three tasks from this task's critical path and lets
the extractor, the rules and the differential proof land while #387 is still in flight.

## Alternative framing 3 — generate the board instead of diffing it

#297's title says a drawn CPU becomes a **buildable** 74-series breadboard, not a twice-drawable one.
#394 already produces a `PackPlan`: refdes, section assignment, BOM, wiring list. Given that, the tool
can *place* — auto-assign parts to rows and emit a jumper list — and then Π_phys = Π_sch by
construction and there is no discrepancy to report. The student's artifact is a printed wiring list
they follow at the bench, which is what a lab actually consumes.

Under that framing the consistency check earns its keep only for hand-placement, which is a real but
much narrower case, and it is worth saying out loud that auto-placement plus a wiring list may deliver
more of CAP-04 than the entire FEAT-043 canvas programme. I would want the maintainer to decide that
before TASK-0092 spends weeks in `SimpleEditor` (5,852 lines, deliberately unfloored per
`pom.xml:408`).

## What I would keep verbatim

- **H1's one-type discipline** and §10's refutation move ("extend `NetPartition` deliberately, once,
  for both derivations — do not give the physical side its own type"). Exactly right, and the same
  argument applies to every provider in framing 1.
- **P7 and the `SCHEMATIC` default.** Non-negotiable and correctly argued.
- **The refusal to emit C6 speculatively.** The strongest paragraph in the issue; it is the same
  posture sweep-06 takes toward the `when others` arms and the `reg = 4'h0` initializer.
- **§7.9's ordering rule** — synthetic `Wire` insertion order as a declared function of tie-point
  address, never placement order. That is the kind of constraint that prevents a determinism bug
  nobody would ever find.

## One design objection under any framing

A per-circuit *saved* binding means a `.jls` file emailed to a grader can simulate differently with no
visible cause. The issue defends per-circuit over per-view, and that comparison is right, but the third
option is unexamined: make the choice a **run parameter** (flag, or an explicit selection the GUI shows
as a permanent banner, the way the disabled-`SubCircuit` banner already works), and persist nothing.
Invisible durable state that changes simulation results is the one thing a grading tool must not have.

## Recommended re-cut

Disregarding the stated acceptance criteria as a package — not because any single one is wrong, but
because they bundle four independently valuable things behind one 10-week prerequisite:

1. **`NetRules` over any single partition** (C4, C5, C6, plus sweep-06 G's undriven inputs,
   combinational loops, width mismatches). Runs on the schematic partition *today*, with no canvas, no
   file-format change and no new view. C6 stays gated on #387; C4/C5 and the G rules do not.
2. **The partition-provider seam**, replacing `PhysicalBinding`, carrying P6/H3 as its standing
   invariant.
3. **`PhysicalNetExtractor` over a text wiring list**, per framing 2 — the whole of this issue's
   research question, with the canvas off the critical path.
4. **`PartitionDiff`** — C1 and C2 only, genuinely a set operation over blocks, ~200 lines, and C3
   moved to whatever owns `PackPlan` since it is a placement-coverage check, not a net check.

That ordering delivers this issue's actual value earlier, gives #387 a consumer that does not wait on
three editor tasks, and puts the work back on the arc `docs/capability-roadmap/sweep-06-physical-boundary.md`
already drew.
