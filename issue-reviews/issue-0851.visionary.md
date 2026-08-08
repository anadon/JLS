# Issue #851: TASK-C370-6: the editor reads the flat state through a view proven in agreement, and per-edit cost and startup time do not regress — the criterion that can veto the whole feature
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Two things are bundled under one task id, and they are not the same size or the same kind.

1. **A veto.** "A capacity win that costs per-edit responsiveness is refused, not traded." This is
   the best sentence in the whole #370 tree — the adversarial boundary comment on #370 says so too,
   and it is right. JLS's identity per README.md is a classroom editor first (Poplawski's tool,
   students drawing gates); #312's 10^10-element capstone is a different program wearing the same
   binary. The veto is where the project's actual priority order is written down.
2. **A reconciliation chore.** "The editor's model reads go through the flat state or a view." This
   half rests on a conflation that the code makes visible, and it is the half I would rewrite.

## The conflation, in measurements from the tree

#370 counts **6.8 runtime objects per logic element** and calls the whole lot "runtime state." In
`src/` those objects are `Element` + its `Put`s + `WireEnd`s + `WireNet`s + a `BitSet`. But what
each holds splits cleanly:

- `src/jls/elem/Element.java:22-50` — `id`, `stableId`, `x`, `y`, `width`, `height`, `uneditable`,
  `highlight`, `savex/savey`, `circuit`. Every field is **edit-time structure or geometry**.
- `src/jls/elem/Put.java:22-44` — `name`, `element`, `xr`, `yr`, `bits`, `touching`, `wireEnd`,
  `face`. Structure and geometry again. Only `Put.java:385` (`currentValue`) is simulation state.

Now measure the editor's exposure to each. Simulation values are read from `jls.edit` at exactly
**four** call sites: `src/jls/edit/DisplayRenderer.java:45`, `src/jls/edit/InteractiveSimulator.java:887`
and `:1004`, `src/jls/edit/MemoryContentsDialog.java:64`. Structure and geometry are read from
**65 of the 85 files** in `src/jls/edit/` (31 of them `*Renderer`), plus all of
`src/jls/SpatialIndex.java`, whose index key is `Element.getIndexBounds()`.

So AC-1 is either a half-day of accessor plumbing or a rewrite of the entire editor, and which one
depends on a scope decision (#370 Open Question 3, unanswered) that #851 inherits without stating.
The task's 3-5 mw band is only coherent for the first reading; the ≤150 B/element budget is only
reachable under the second, because the headers being counted are on the geometry-carrying objects.

The conflation is already producing text that cannot be executed. **#851 AC-2 and #850 AC-2 both say
"mutate runtime state and assert the editor's / the spatial index's read reflects it with no explicit
sync."** For the spatial index that assertion is *wrong on purpose*: mutating a signal value must not
move an element's bounding box. `SpatialIndex`'s own doc comment says it is "deliberately forgiving
about staleness" and rebuilds from the authoritative element set — it is a derived cache, and
requiring it to track runtime state confuses derivation with divergence.

## Alternative A (the one I would build): cut at elaboration and at workload, not at the editor

The seam already exists and is load-bearing: `Circuit.load` → `finishLoad` builds the editable object
graph; `Simulator.initSimulation` (`src/jls/sim/Simulator.java:177-201`) walks it calling `initSim`
on every `LogicElement`. Make the flat columns the **product of elaboration**, derived one-way from
the object graph at simulation start, and let the interpreter walk them.

What that buys, against #370's own invariants:

- **Invariant 4 is satisfied by construction, not by a test.** A one-way derived representation
  regenerated from an authoritative source is not "two representations kept in step by discipline" —
  it is the same relationship `SpatialIndex` already has with the element set, and the same one
  `CircuitSnapshot` has (undo is deflated save-text re-loaded through the ordinary path,
  ARCHITECTURE.md "The save/load pipeline"). JLS's existing answer to "two things must agree" is
  *derive one from the other and invalidate*, twice over. #851 proposes a third pattern where the
  project already has a house pattern that works.
- **Invariant 3 cannot fire.** The editor's per-edit path is untouched, so per-edit cost is
  structurally unable to regress. Only startup/elaboration time is at risk, which is one number on
  one path instead of a responsiveness surface across 65 files.
- **Capacity lands where capacity is needed.** Nobody draws a 10^8-element circuit with a mouse.
  README.md already ships a headless-only surface — `ghcr.io/anadon/jls`, batch `-b`, VCD, autograders,
  "headless by construction (no display stack)". At capstone scale the elaborated columns are the
  *only* representation resident, because no editor is running; below that scale both exist and the
  footprint of the small graph is irrelevant. The two representations never co-occur at a size where
  co-occurring costs anything. #370 spends four task rows fighting a problem created by insisting on
  one representation for two workloads that do not overlap.

The honest cost of Alternative A: peak heap during an interactive simulation of a mid-size design is
graph + columns, and #370's ≤150 B/element must then be declared on the elaborated path, not on the
loaded circuit. I think that is the right place for it anyway — K17-1 is a capacity criterion about
what fits, and what fits is measured where no GUI exists. It also needs an explicit note against the
recorded decision in ARCHITECTURE.md ("Simulation execution strategy: discrete-event interpreter is
the sole strategy", #221): elaborating to a flat *layout* is not the levelized/compiled *evaluation*
that decision refuses, and the distinction should be written into that section rather than left for a
reviewer to infer.

## Alternative B: the veto should be a ratchet, not a committed before/after

AC-3/AC-4 produce a data file at one commit. JLS's own idiom for "this property must never regress"
is a standing test: `HeadlessCoreRatchetTest`, `NotificationRatchetTest`, `CollabSecurityRatchetTest`,
`SocketConfinementRatchetTest`, `NullMarkedRatchetTest`, `PackageInfoRatchetTest`,
`PointerApiRatchetTest` — seven of them in `test/jls/` already. A one-shot comparison catches the
regression #846 introduces and nothing after it: #476's calendar queue, #879's value plumbing, the
collab op path, and #332's partitioned model all cross the same responsiveness surface later.

Concretely: fold AC-3/AC-4 into an `EditorResponsivenessRatchetTest` owned by the measurement gate
(#335), landing **before** #846 rather than after it, with committed thresholds on named fixtures. The
veto then belongs to the repository instead of to one issue — which is exactly what the #370 boundary
comment asked for when it said "that veto should follow the work rather than the issue number." As a
side effect, "before" numbers exist on master before the migration begins, so #846 gets feedback while
it is being written instead of at #851's review.

## Alternative C: "provably in agreement" already has a literal meaning here

#370 OQ3 says "provably must mean a test, not a code comment," and settles for a test. This project
does better than that in exactly this situation: `proofs/SpatialIndexCorrectness.agda` carries THEOREM 1
(query parity) with `test/jls/ProofBridgeTest.java` pinning its modelling assumptions to the class, and
`test/jls/DrawCullingParityTest.java` asserts the culled draw set equals the brute-force set. If a view
is genuinely needed (i.e. if Alternative A is rejected), the agreement proposition should be written in
that same shape — a parity property over the whole golden corpus — and the cheapest oracle is one the
repo already owns: serialize through the editor's read path and through the columns, and assert the two
save texts are byte-identical (`DeterministicSaveTest`, `CircuitRoundTripTest`, `CircuitSnapshot`).
That is a total-agreement assertion over every field of every element, versus AC-2's single
hand-constructed divergence. Note also that the save format already achieves **~96.6 B/element on disk**
— the existence proof that ≤150 B/element is reachable is sitting in `FileAbstractor`, and the columns
are close to being an in-memory dialect of a format the project has already specified
(`docs/file-format.md`).

## Where this pulls against the larger arc

`SimpleEditor` is 5,852 lines and #84's residual (the 9-state mouse machine) is still open. As written,
#851 makes the flattening the forcing function for touching that class — the largest, least-tested,
most pedagogically load-bearing file in the tree — on behalf of a capstone no user has asked for.
Alternative A keeps that file out of the capacity program entirely, which is the correct alignment:
`SimpleEditor` should be decomposed when #84 is funded and for #84's reasons, not incidentally by a
memory-layout migration whose invariant 3 makes any editor mistake fatal to the feature.

## What I am disregarding, explicitly

- **AC-1 as written.** "The editor's model reads go through the flat state or a view" is unexecutable
  until someone says whether geometry is in the columns. Replace with: *the columns hold simulation
  state only; the editor's four value-read sites route through the column accessor; geometry stays on
  the element objects and is out of #370's scope.* If the answer is instead "geometry is in the
  columns," then #851 is not a 3-5 mw task and #370 needs a re-band before it is filed as one.
- **AC-2 as written**, and #850's twin — replace the hand-built divergence with the corpus-wide
  save-text parity property above, and drop the "spatial index reflects a runtime mutation"
  requirement, which asserts something false about a geometric index.
- **AC-3/AC-4 as a one-shot** — promote to a standing ratchet landing before #846 (Alternative B).

Keep AC-5 unchanged; the headless boundary is the one thing here that is already right.
