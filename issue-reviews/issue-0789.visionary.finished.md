# Issue #789: TASK-C570-3: editor accelerators are viewable and rebindable in a settings surface, persisted across sessions, with a reset-to-defaults path
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Two things are bundled under one title, and they have very different value to JLS.

1. **Make the binding model singular and machine-readable.** AC-1 and AC-5 ("sourced
   from the shared `Action`/`EditOp` layer", "a test asserts bindings resolve through
   one path"). This is infrastructure JLS wants regardless of whether any user ever
   rebinds a key.
2. **Let a user rebind keys, persisted, with conflict detection and reset.** AC-2/3/4.
   This is the Digital-parity item — and #787 (TASK-C570-1, `ordering_after`) is an
   explicit gate that may *cut* it: "an item that would ship only to poach a
   competitor's users is cut here, in writing (KC-30-1)."

#789's own Outcome paragraph pre-answers that gate — it opens "User-configurable
keybindings — the request Digital rejected", which is the competitor framing KC-30-1
tells the project to strip. The task is written as though #787 has already returned
"yes". It should be written as contingent, and (2) should be the contingent half.

## The stated foundation does not exist yet — and AC-3 proves it

AC-1 says every accelerator can be sourced from the `Action`/`EditOp` layer. At HEAD
the accelerator surface is fractured across four sources:

- `src/jls/edit/EditOp.java:110` — a hand-written `switch` over 18 editor ops.
- `src/jls/MenuAcceleratorPolicy.java` — File New/Open/Save/Save-As/Exit as five
  separate static methods (`newCircuit`, `open`, `save`, `saveAs`, `exit`), plus
  mnemonics. These are *not* `EditOp`s.
- `src/jls/JLSStart.java:1711,1716,1798,1815,1832,1849` — Simulator Run/Stop as
  literal `KeyStroke.getKeyStroke("F5")`/`"F7"`, and the four View/zoom accelerators
  as inline `VK_EQUALS/VK_MINUS/VK_0/VK_9 + mask`. Neither goes through any policy.
- `src/jls/edit/SimpleEditor.java:1367-1464` — the canvas `InputMap`, which re-spells
  the *same* strokes as literals (`KeyStroke.getKeyStroke(KeyEvent.VK_W, menuMask)`,
  `VK_M`, `VK_P`, `VK_T`, `VK_A`, `VK_E`, `VK_X`, `VK_C`, `VK_V`, `VK_Z`, `VK_L`)
  rather than calling `op.accelerator(osName)`. Menu and canvas agree today by
  discipline, not by construction; `EditActionMatrixTest:115` catches divergence only
  because it is `@Tag("display")`.

So AC-1's "a newly added operation appears without editing the surface" is false today
in the direction that matters: adding an op means editing the `EditOp` switch *and*
the canvas `InputMap` *and*, if it is a File/View/Simulator op, a literal in
`JLSStart`.

AC-3 is where this becomes structural rather than cosmetic. "A binding that would
shadow another is refused at entry, naming both operations" **cannot be honestly
implemented over `EditOp` alone.** The interesting shadowings are exactly the
cross-domain ones: rebinding something to mask+S shadows File>Save (not an `EditOp`);
to mask+`=` shadows Zoom In (a `JLSStart` literal); to an arrow key or Enter shadows
`KeyboardConstructionPolicy`'s caret bindings (`SimpleEditor.java:1472+`); to Escape
shadows end-wire (`:1422`). This is the 2026-07-19 hold from #75 restated: advertised
and effective must agree — and the hold arose precisely because the occupant
(canvas mask+W Watch) lived in a different table from the advertiser (File>Close).
**AC-3 forces the registry AC-1 only gestures at.** That is the real content of this
issue, and it is worth doing on its own.

## The reframing: cut at "commands", not at "keys"

Build `jls.edit.CommandRegistry` (Swing-free, headless-testable, in the
`MenuAcceleratorPolicy` idiom): one row per user-invocable command — stable id,
label, default stroke as a pure function of `os.name`, dispatch scope
(`WHEN_FOCUSED` vs `WHEN_IN_FOCUSED_WINDOW`), help topic anchor, and #91 component
name. Every existing source becomes a *consumer*:

- `JLSStart`'s menu builders and `SimpleEditor`'s `InputMap`/`ActionMap` are both
  generated from the registry — one table, two projections. `EditActionMatrixTest`
  then verifies a tautology instead of a coincidence, and AC-5 falls out for free.
- `HotkeysHelpAccuracyTest` (`test/jls/HotkeysHelpAccuracyTest.java`) stops being a
  hand-maintained `DOCUMENTED_OPS` map of 17 rows and becomes a generator check:
  the hotkeys table in `resources/help/editor/editing/hotkeys.html` is emitted from
  the registry, so help *cannot* drift rather than being caught drifting.
- #75's Open Question 1 (fate of canvas mask+W, blocking File>Close's accelerator)
  becomes a query against the registry, decidable mechanically, not a judgment call
  held open for a month.
- The registry is the natural `jls.module.ExtensionPoint` this project's #223 seam
  catalog does not yet have (`docs/extension-points.md` has no keymap/command seam).
  A future module contributing an element or an exporter needs a menu entry and a
  binding; today it would have to reach into `JLSStart`. Declaring `COMMAND` as a
  typed point puts this on the recorded architectural arc instead of beside it.

That work is ~1 MW of the 1.5-2 band, it is unambiguously merit-justified under D10
(it removes a whole class of shadowing bug and a whole class of doc drift), and it
needs no #787 verdict to proceed.

## The out-of-the-box alternative: a command palette, not a rebinding table

I am explicitly setting aside AC-1's "a settings surface lists every editor
accelerator" as the deliverable, and here is why.

Ask what a user who wants to rebind actually wants. Three populations:

- **Migrants from Logisim/Digital** wanting muscle memory. This is the poaching
  motive KC-30-1 says to cut, and per-key rebinding serves it badly anyway (nobody
  hand-enters 20 strokes).
- **Non-US keyboard layouts**, where a default stroke is physically awkward or
  unreachable. This is the one genuinely merit-based case — and the AC as written
  *excludes it*, because the bindings that break on non-US layouts are `Ctrl+=`,
  `Ctrl+-`, `Ctrl+9/0` (View/zoom) and F5/F7 (desktop-grabbed), none of which are
  `EditOp`s and none of which AC-1's stated source can see.
- **Everyone else**, who does not know the operation exists. #73's audit finding U5
  is "everything important is hidden in hover and right-click" — a discoverability
  problem, not a binding problem.

A searchable command palette over the same registry (one dialog, filter box, list of
command + live binding, Enter invokes) serves populations 2 and 3 outright: it is
keyboard-layout-independent because you type *words*, it is a first-class
accessibility win for #75's arc, it makes every operation discoverable for #73, and
it never breaks the "advertised == effective" invariant because it shows the live
binding by construction. It is cheaper than a conflict-resolving rebinding table plus
persistence plus corrupt-value fallback plus per-user help divergence.

If rebinding still clears #787's gate, ship it in the shape the codebase already
uses for exactly this: **named keymap schemes**, mirroring the `Color scheme`
`JRadioButtonMenuItem` group in `JLSStart.globalMenu()` (`:1941`) with
`prefs.rememberTheme` → `prefs.rememberKeyScheme`. A scheme is a whole consistent
set, so conflicts are checked once at authoring time rather than by a conflict UI,
help can document each scheme, and "reset to defaults" is "pick the default scheme".
Per-key override becomes a later, small opt-in for the residue schemes cannot serve.

## What the issue pulls against

- **Every artifact that names a stroke becomes conditionally true**: `hotkeys.html`,
  `keyboard.html`, the tutorial, `docs/keyboard-a11y-verification.md`'s invocation
  matrix, #73's planned README screenshots, and any TA saying "press Ctrl+Z" in a
  lab. AC-4 quietly concedes this by demoting `HotkeysHelpAccuracyTest` to "still
  passes *against the default set*" — i.e. help is now truthful only for users who
  never rebind. For a course-support tool that is a support cost transferred to
  instructors.
- **It matches the shape of a decision the project already made against itself.**
  ARCHITECTURE.md records i18n as a non-goal: "a large, ongoing tax with no
  requesting user". Per-key rebinding for a single-maintainer pedagogy tool used a
  few weeks per term has the same profile, and its requesting user is currently
  *another project's* issue tracker. The same discipline should apply, and #787 is
  the place to apply it.
- **It would create JLS's first settings window as a side effect.** There is no
  Preferences dialog; `globalMenu()` is a pile of one-off items each with its own
  listener and its own `UserPrefs.rememberX`. Either own that deliberately (one
  Preferences dialog absorbing theme, grid/background color, undo depth, and keymap,
  retiring the Global one-offs) or do not build a "settings surface" here at all.
  Doing it accidentally, inside a keymap task, is the worst of the three.

## Concrete design notes if this proceeds as scoped

- `UserPrefs` (`src/jls/UserPrefs.java`) is flat: four constants, `get`/`put`/`remove`
  on one node. Give the keymap a child node (`node.node("keys")`), so
  reset-to-defaults is `removeNode()` — one call, atomic — rather than enumerating
  every command. AC-2's corrupt-value fallback then mirrors the existing
  `parseColor`/`parseUndoDepth` pattern per entry, which is already the house style.
- Persist the *stroke*, not the mask-resolved chord, or a keymap written on macOS
  and synced to Linux (`Preferences` backends do sync in some lab setups) resurrects
  Meta modifiers that no Linux keyboard produces. Store a canonical
  `KeyStroke.getKeyStroke(String)`-round-trippable text form and re-resolve the
  platform mask on load.
- A rebinding that lands on a plain letter must be checked against
  `KeyboardConstructionPolicy` and the plain-key canvas bindings (R/Shift+R/F/W/V),
  not just against other menu accelerators — another reason the conflict domain is
  the registry, not `EditOp`.

## Recommendation

Split #789. File the registry/generation half as the task that runs now — it
discharges AC-1, AC-3's precondition, and AC-5, unblocks #75's Open Question 1, and
earns its D10 justification without reference to any competitor. Hold the rebinding
half explicitly contingent on #787's verdict, and when that verdict is written,
compare per-key rebinding against the two cheaper designs above (command palette;
named schemes) rather than treating the Digital wishlist as the specification.
