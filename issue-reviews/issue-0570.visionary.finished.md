# Issue #570: FEAT-C30-5: the three features Digital's users spent years asking for — dark mode, diving into a live subcircuit mid-simulation, and rebindable keys — exist in JLS on their own merit
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of provenance, #570 asks for two things: **you can see inside a
running subcircuit**, and **you can change a key binding and have it stick**.
Both are real wants. Neither is what the issue is organized around.

The organizing axis is *provenance* — "Digital's users asked for these" — and
that is a marketing bill of materials, not an engineering unit. The issue
concedes this in its own acceptance criteria: AC-1 is a null (dark mode is
#289's, "consumed"), AC-3's storage and its dialog belong to #76's planned
settings surface and its policy layer to #75 (per the consolidation comment),
and AC-4 is a filing procedure. Exactly one criterion, AC-2, is both new and
unowned. A feature whose ACs are one no-op, one shared, one procedural and one
real is a bundle wearing a feature's clothes; KC-30-1 ("each must stand on its
own JLS merit") is, read honestly, the argument for dissolving it — three
items that each stand alone do not need to be one issue, and PF-6 already owns
the "tell the people who asked" job.

**I am disregarding AC-1 and AC-4 as written.** AC-1 should be deleted, not
consumed: a criterion asserting that this issue does no work here creates a
dependency edge with nothing behind it and will confuse close-out. AC-4 belongs
in the two child issues, where a path-and-cost argument can actually be made
against code.

## Reframing 1 — AC-2 is a core observability feature, not a GUI gesture

JLS already has hierarchical addressing and already observes subcircuit
internals live. `LogicElement.getFullName()` (`src/jls/elem/LogicElement.java:511`)
walks `isImported()`/`getSubElement()` to produce `sub1.sub2.elem`.
`InteractiveSimulator.findTraces` recurses through `SubCircuit` at
`src/jls/edit/InteractiveSimulator.java:990` and names every trace by full
path (`:1000`). `BatchSimulator.findWatched`/`findProbes` do the same
(`src/jls/sim/BatchSimulator.java:223,253`). So a student *can* watch a nested
register live today — in the trace window. The unmet want is **spatial**: the
schematic, not the waveform.

Cut along that seam and the concept generalizes into something worth more than
one gesture. What is missing from `core` is a first-class **scope path** — an
addressable, per-instance handle into the elaborated hierarchy — where today
hierarchy is stringly-typed into a dotted name at the point of use. The
immediate proof that this is a real gap and not an aesthetic one:
`BatchSimulator.toVcd()` emits a single flat `$scope module <top>` and dumps
every signal as a dotted name inside it (`src/jls/sim/BatchSimulator.java:424`,
keys from `getFullName()` at `:398`). GTKWave and Surfer therefore show a flat
list where the standard would give a browsable tree. One `ScopePath` concept in
`core` pays four consumers: nested `$scope`/`$upscope` in VCD (a genuine
`docs/batch-interface.md` improvement, and the thing that makes #200/#201/#202
RV32I traces navigable), a hierarchy tree in the interactive trace window,
addressable watch targets for `-t` vectors, and — as one GUI consumer among
several — the live dive. Framed as a GUI gesture, AC-2 serves one of the
project's three funded trajectories (grand-architecture §2); framed as scope
addressing, it serves all three, and it lands inside the headless kernel where
#77 wants weight to accumulate.

## Reframing 2 — the dive is blocked by a conflation, not by missing machinery

The nine-year-old version of this problem is hard in Digital's codebase. In
JLS it is mostly a category error that is already half-corrected:

- Diving today *is editing*. `SimpleEditor.doModify` opens a second `Editor`
  on the child `Circuit` and then disables the parent
  (`src/jls/edit/SimpleEditor.java:5159-5195`, `disableForSubcircuit` at
  `:720`, #86 H2). Navigation is welded to an exclusive mutation mode.
- Simulation disables the editor for the whole run
  (`InteractiveSimulator.java:638`) **while still repainting it** (`:696`).
  The render path already runs live against changing values.
- Rendering is already editor-independent: `CircuitRenderer.of(Circuit)`
  (`src/jls/edit/CircuitRenderer.java:68`) and
  `draw(Graphics, Set<Element>, @Nullable SimpleEditor)` (`:86`) will draw any
  circuit with a null editor.

So the correct move is not "make the editor safe mid-simulation" — it is
**never open an editor**. A read-only `CircuitInspector` pane over a `Circuit`,
repainted on the *existing* batched, rate-limited channel that already governs
the clock display (grand-architecture §6's hot/cold-plane rule — never per
signal event), delivers AC-2 without touching mutation, undo, or the state
machine. `SubCircuit.copy()` deep-copies the child `Circuit`
(`src/jls/elem/SubCircuit.java:332-338`), so each instance owns its own live
state and "this instance" is already well-defined — no elaboration work needed.
One caveat to carry into the design: `Circuit.getSubElement()` is a single
parent back-pointer, which makes the tree navigable upward but forecloses a
shared subcircuit *definition* with multiple instance paths (which #212 and any
library story will eventually want). Address the inspector by `ScopePath` from
the top, not by that back-pointer, so the cheap version does not entrench the
limitation.

A staged version follows naturally: **demo slice** = open the existing
subcircuit tab in view mode during a run (`enabled=false`, no parent-disabling
banner, repaint on the rate-limited channel) — a small change delivering AC-2's
user-visible outcome. **Generalization** = `ScopePath` + nested VCD scopes +
inspector, funded when the CPU trajectory demands it. That is a real
path-and-cost story, which AC-4 asks for and the issue does not supply.

## Reframing 3 — AC-3's deliverable is a value, not a dialog

#75 already gives `EditOp` (18 operations), `MenuAcceleratorPolicy`,
`KeyboardConstructionPolicy`, and `HotkeysHelpAccuracyTest` pinning
`hotkeys.html` against `EditOp` drift; `UserPrefs`
(`src/jls/UserPrefs.java:31-38`) is the store. What is missing is the middle
term: a **`Keymap` as a first-class value** — `EditOp → KeyStroke`, resolved as
`defaults(policy) ⊕ user overrides` — with the *existing* generators (menu
accelerators, canvas `InputMap`, popups, `hotkeys.html`) reading the keymap
instead of the policy directly.

Build that first and three things fall out for free. The settings page is a
table view over the value and "reset to defaults" is `overrides.clear()`. The
consolidation comment's constraint 3 stops being a procedural warning and
becomes structural: a rebinding UI *cannot* bypass policy, because it writes
the keymap, not menu items, and keymap-aware `hotkeys.html` generation is a
consequence rather than an extra task. And — the out-of-the-box payoff — #75's
Open Question 1, the mask+W Watch-vs-Close adjudication that has blocked its
close-accelerator slice since July, becomes a **conflict-detection function
over the keymap** instead of a one-off human ruling: shadowing is computable,
testable headlessly, and reportable to the user at rebind time. AC-3 as
written buys a dialog; AC-3 reframed retires a stalled decision on a sibling
feature and makes every future binding change self-checking.

## Where it pulls against the arc

One tension deserves to be stated loudly. CAP-37 (#521) sets KC-37-1 — nothing
in its features lands inside `SimpleEditor`, ordering behind #316/#84 — and
CAP-30's own AC-5 requires the largest file in `jls.edit` under 1,500 lines.
`SimpleEditor.java` is **5,852 lines today**, and the cheapest implementation
of AC-2 is a new branch in `doModify` at line 5159. As written, this issue's
easy path damages its own parent capstone's acceptance criterion. #570 must
inherit KC-37-1 explicitly; the inspector-pane framing satisfies it by
construction, since a new component is decomposition rather than accretion.

Secondary: AC-2 should order with or behind #441 (headless interaction machine)
so it is assertable in the #91 harness rather than by screenshot; AC-3 should
order behind #76's settings dialog. Neither should order behind #289 at all.

## Recommendation

Endorse the underlying goals; dissolve the container. Delete AC-1, demote AC-4
into the children, and split into two feature issues that each carry their own
merit argument: **live hierarchical observability** (core `ScopePath` + nested
VCD scopes + a read-only inspector pane, staged as above, KC-37-1 inherited)
and **the keymap as a value** (resolved `Keymap`, conflict detection, then the
settings page as a view). Leave #570 open only as the CAP-30 PF-5 roll-up that
PF-6 markets from — or close it and let the capstone reference the two children
directly. Its unique contribution to the tracker is the observation that these
items are wanted; that observation is now recorded and does not need an issue
of its own to survive.
