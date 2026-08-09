# Issue #616: TASK-C487-4: the routed length comes back — a board's real geometry returns as a datum about the same net, so the lint judges the number the board actually has
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this task is actually for

One sentence survives compression: **a check should judge what was manufactured, not
what was intended, and the difference between the two should be visible.** That is a
good thing for a teaching tool to be able to say, it is the only part of FEAT-060 (#487)
that keeps value even if K18-2 fires and the external DRC claim gets narrowed (#487's own
re-planning protocol says exactly that), and it is cheap in substance: a scalar per net.
I endorse building it.

What I do not endorse is the sentence the task is named after. "A second view's datum
about a first view's net, over FEAT-014's (#318) per-view addressing" is a metaphor from
the parent's decomposition that has been promoted into an architecture, and it is the
wrong one. Everything below follows from that.

## The seam is provenance, not view

FEAT-014 (#318) exists for a specific thing: *the same artefact drawn in two places by a
human*. Its whole payload is a schematic position and a breadboard position for one
component, coexisting, each editable, each mutated by a view-discriminated op with an
exact inverse, each mergeable under a CRDT (#318 §1, §3; `blocks: [329]`, the breadboard
canvas). Its unit is `(x, y, width, height, orientation)`.

A routed length is none of that. It is not a position, it is not editable in JLS, no op
mutates it, it has no inverse, it does not merge, and no user drags it. It is a *number
an external tool measured about an artefact JLS does not own*. The only property it
shares with breadboard geometry is "there is more than one of them per net" — and that
plurality is on a different axis. The task's own AC-2 names the axis correctly and then
files it under the wrong mechanism: "one is authored intent, the other is measurement."
That is **provenance**, and provenance is orthogonal to view. A declared length and a
routed length both belong to the schematic net; a breadboard position and a schematic
position are both authored.

Cutting along provenance instead has consequences worth having:

- **The #318 dependency disappears.** #318 is blocked by #319 and #337, carries an 11–17 mw
  band, and has an unresolved open question about widening a sealed `permits` list to give
  `WireNet` an identity at all (#318 OQ-1; `src/jls/elem/WireNet.java:17-30` is still a
  plain class with five fields). This task does not need `view:instancePath:sid`. It needs
  a key — and AC-1 already names the right one: FEAT-004's stable net naming (#336), whose
  own Intended Audience section was written for precisely this consumer: *"Anyone keying an
  external annotation on a JLS net name — SAIF, SDF, an external-simulator VCD comparison."*
  #336 was designed to be this task's key. #318 is inherited scope, not required scope.
- **AC-4 stops needing a test.** "Only lengths, not traces/vias/placement" is currently
  guarded by a test or a documented refusal. Under an overlay of named scalars there is
  nowhere to put a polygon; under a per-view *geometry* section there is a natural place
  for one, and the guard rail exists because the framing points down the slope the parent
  explicitly fears ("would make JLS a layout tool by accident"). The better design makes
  the refusal structural.
- **It generalizes toward things JLS already owes.** Back-annotated SDF delays (#89),
  Liberty slew/load (#87), measured fanout and DC loading (FEAT-041), a differential run
  against an external simulator — every one is "an external tool measured something about
  my design; keep it beside the authored value; never overwrite; let a check re-run against
  it." Built as a per-view geometry consumer inside a PCB feature, this becomes the first
  of five private implementations of that idea — the same failure #336 exists to fix on the
  partition side (three copies of one connected-component walk).

## The carrier: a sidecar, for the reason a sidecar is wrong for constraints

#487 §2 rejects a sidecar as the constraint carrier, correctly: an authored constraint is
part of the design and must travel with it. That rejection is being inherited by this task
by proximity, and it inverts here. **A routed length belongs to a board revision, not to a
circuit.** Store it in the `.jls` and the first re-route silently turns the file into a
carrier of a false measurement that `-check` will then judge confidently. The failure mode
of the sidecar (stale file) is *visible*; the failure mode of the embedded section (stale
section that looks authored) is exactly the "silently unmanufactured requirement" hazard
this programme is otherwise so careful about — pointed the other way.

The precedent is in tree and is a close structural match: `src/jls/hdl/board/PinBindings.java`
plus the `-pins` flag (`src/jls/JLSStart.java:759-787`) is a trivially-shaped text sidecar,
keyed by port name, parsed at the CLI, never stored in the circuit, with every malformed
line reported at once. It is the "data-not-code" precedent #487's own evidence section
cites for the emitter; it is the better precedent for the reader.

That also settles a question AC-1 never asks: **from what format?** "A routed per-net length
imports" is silent on it, and the honest reading is a `.kicad_pcb` parser in Java — a format
JLS would then track across upstream versions, against the recorded stance "orchestrate
external tools, never reimplement" (`docs/grand-architecture.md` §2, §9). The correct split
is: a two-column text sidecar JLS reads, and a `scripts/kicad-lengths.py` extractor that
produces it — the same shape as `scripts/icestick-handoff.sh`.

## Two acceptance criteria I am explicitly disregarding

**AC-3, "the verdict computed from the routed number *instead of* the declared one."** This
throws away the moment the whole rung exists for. The pedagogically load-bearing output is
not the routed verdict; it is the *pair*:

```
net CLK: declared 50.0 mm  -> ratio 0.9, lumped model valid
         measured 71.4 mm  -> ratio 1.3, lumped model NOT valid   [routed.len, kicad 9.0]
```

Intent said fine, the board says otherwise, and the student sees the gap rather than a
number that quietly changed meaning. Keep AC-3's *differing verdict* requirement; drop
"instead of."

**AC-1's "stored in its own per-view section" and AC-5's fallback rule.** Under an overlay
there is no precedence to define: `-check` reports every datum it holds, each labelled with
its provenance, and "not assessable" remains what it already is when nothing is declared.
AC-5's "falls back to the declared value" is a substitution rule invented to repair a design
that stores two numbers in one slot. Remove the slot and the rule is unnecessary.

## The criterion the task is missing, and which the reframing makes cheap

Nothing here says how JLS knows a measurement describes *this* circuit. Rename a net, re-wire
a branch, re-route the board — the lint will judge the old number without hesitation. #166
(canonical serialization, closed) already gives a content digest for free; record it in the
sidecar header and `-check` can say `measurement taken against a different revision — not
assessable` instead of asserting. FEAT-058 (#486) has already made vacuity a first-class
verdict, so staleness lands in a vocabulary that exists. This is a one-line addition under
the overlay and is close to unstatable under "a per-view geometry section."

## Concrete alternative, in full

- **Carrier:** `routed.len` — `# design: <#166 canonical sha256>` header, then `<net name>
  <value> <unit>` per line, `#` comments, net names from #336's frozen convention.
- **Reader:** a leaf class beside `PinBindings`, reporting every malformed line at once.
- **CLI:** `jls -check -measured routed.len design.jls`.
- **Report:** one block per net, declared and measured lines each labelled with provenance
  and source; `not assessable` for absent inputs and for digest mismatch.
- **Producer:** `scripts/kicad-lengths.py`, outside the Java build, versioned with the rig
  scripts.
- **Prerequisites:** #336 only. Not #318, not #319 for this half.
- **Seam:** `docs/extension-points.md` has a pending `hdl.importer` row and no annotation
  row; if a seam is wanted, this is where the measurement-overlay contract gets named
  before someone invents a second one.

## What I would keep exactly as written

"Lengths only in v1" is right and is the best judgement in the issue. AC-2's insistence that
declaration and measurement never merge is right. The instinct to make the return path
independent of the emitter and the external DRC (#487 §6 already says back-annotation is
concurrent with all three) is right, and the reframing strengthens it: with #318 dropped,
this becomes the *first* buildable piece of FEAT-060 rather than the last, and the piece
that survives if the DRC claim narrows.

## Verdict

**endorse-with-reframing.** The outcome is worth building and worth building early. The
framing — routed length as per-view geometry over #318's addressing — misclassifies a
measurement as a location, imports an 11–17 mw unshipped prerequisite this task does not
need, stores a board-revision fact inside a circuit file, and buys guard rails that a
provenance overlay gets for free. Re-file it as "measurement annotations, keyed by stable
net name, carried in a sidecar, reported beside the authored value," and #616 gets smaller,
lands sooner, and leaves behind the mechanism #87, #89 and FEAT-041 will each otherwise
build privately.
