# Issue #789: TASK-C570-3: editor accelerators are viewable and rebindable in a settings surface, persisted across sessions, with a reset-to-defaults path
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The task's connective tissue to #75 and #570 is well cited, and two of its
pieces (persistence-with-fallback, and the default-set regression test) rest
on real, working precedent in the tree. But its central premise — "one
binding model... rather than a second one bolted beside it" — is false
against the current code, and that falseness undermines AC1, AC3, and AC5
simultaneously. The task is sized (band_mw 1.5-2) as if it only has to
expose an existing unified model; it would actually have to build that
unification first.

## Findings, most severe first

**1. [HIGH] The "one binding model" the task builds on does not exist yet — the canvas key bindings are a hand-duplicated second copy of `EditOp.accelerator()`.**
`src/jls/edit/SimpleEditor.java:1365-1444` registers the canvas's
`WHEN_FOCUSED` `InputMap`/`ActionMap` with literal `KeyStroke.getKeyStroke(...)`
calls — e.g. line 1367 `getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_W,menuMask),"watch")`
— reconstructed independently of `EditOp.WATCH.accelerator(osName)`'s own
switch case in `src/jls/edit/EditOp.java:116-117`, and using a locally
fetched `Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()` (line
1365-1366) rather than `MenuAcceleratorPolicy.menuMask(osName)`. The two
happen to agree today only because someone kept them in sync by hand. AC1's
claim that a newly added `EditOp` "appears [in the settings surface] without
editing the surface" is already untrue for the *existing* system: adding an
`EditOp` case today requires manually adding a matching `getInputMap().put()`
line here, or the enum entry has no canvas binding at all. AC5's "no second
accelerator scheme is introduced... a test asserts bindings resolve through
one path" is not a small verification step — it's new work to eliminate a
scheme that already exists.
*Recommendation:* Retitle the acceptance criteria to make "collapse the
canvas InputMap duplication into a single accelerator source" its own
explicit, sized sub-task before "expose it in a settings surface" is
attempted; otherwise the settings surface will rebind the menu accelerator
and leave the canvas's hardcoded stroke (which wins on focus, see #3 below)
untouched.

**2. [HIGH] "Every editor accelerator" contradicts the codebase: several accelerators are not `EditOp` at all and have no `Action` object to retarget.**
`src/jls/JLSStart.java` wires File>New/Open/Save/Save As/Exit directly off
`MenuAcceleratorPolicy` static methods onto plain `JMenuItem`s (lines
1391, 1409, 1428, 1449, 1574 — `newc.setAccelerator(MenuAcceleratorPolicy.newCircuit(osName))`
etc.), and Simulator Run/Stop are literal, policy-free strings
(`run.setAccelerator(KeyStroke.getKeyStroke("F5"))`, line 1711;
`"F7"`, line 1716). Zoom In/Out/Actual Size/Fit (lines 1798, 1815, 1832,
1849) use yet a third mask source, `Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()`,
bypassing `MenuAcceleratorPolicy` entirely. None of these is an `EditOp`,
so AC1's stated source of truth ("the shared `Action`/`EditOp` layer")
structurally cannot enumerate them. Either "every editor accelerator" is
false advertising and the surface will silently omit File/Simulator/Zoom
shortcuts, or the task quietly requires converting all of these to
Actions/EditOp-like objects first — a substantially larger, unstated scope.
*Recommendation:* State explicitly which accelerator set is in scope
(EditOp only, or literally everything user-facing) and correct AC1's
"sourced from the shared Action/EditOp layer" claim accordingly.

**3. [MEDIUM-HIGH] AC3's conflict rule doesn't mention Swing's binding-scope precedence, which is exactly the mechanism behind the lesson it cites.**
The 2026-07-19 hold this issue quotes ("advertised and effective bindings
must agree") existed because a canvas `WHEN_FOCUSED` binding always wins
over a menu-bar `WHEN_IN_FOCUSED_WINDOW` accelerator on the same keystroke
while the canvas has focus, independent of registration order (#75 body,
"Held then superseded" paragraph). AC3 says a shadowing rebind is "refused
at entry, naming both operations" but never mentions scope/precedence as
part of the conflict test. A same-keystroke-equality check that is scope-
blind will either false-positive on strokes that don't actually collide
(different Swing input-map tiers) or, worse, miss a case where a *newly
rebindable* menu accelerator is silently shadowed by an existing canvas
binding of a different nominal stroke that Swing's precedence still
prefers. As written, a test that merely proves "no two EditOps map to the
same `KeyStroke`" would pass while the effective/advertised mismatch bug
reappears.
*Recommendation:* Make the conflict check explicitly scope-aware (menu-bar
`WHEN_IN_FOCUSED_WINDOW` vs. canvas `WHEN_FOCUSED`) and add a test that
binds a stroke at both levels to prove the *effective* dispatch, not just
stroke equality, is what's checked.

**4. [MEDIUM] Multi-keystroke operations aren't accounted for in the "view/rebind a binding" model.**
`DeleteKeyPolicy.canvasBindings()` (`src/jls/edit/DeleteKeyPolicy.java:56-61`)
binds two unmodified keys (Delete and Backspace) to `EditOp.DELETE`, and
`MenuAcceleratorPolicy.redoBindings()` (`src/jls/MenuAcceleratorPolicy.java:201-208`)
binds two live keystrokes to Redo on macOS — the displayed Shift+Cmd+Z plus
a Cmd+Y "day-one alias" that #75's Global Invariant 2 requires be kept
("rebindings keep the old stroke as a day-one alias where unambiguous").
AC1 talks about "its current binding" (singular) and AC3 about "a binding
that would shadow another" — a 1:1 op-to-keystroke model. The issue never
says what happens to the second stroke when a user rebinds DELETE or REDO:
silently dropping the alias would violate #75's own standing invariant;
keeping it unconditionally could itself become the shadowing case AC3 is
meant to catch. This can be satisfied on paper (rebind the primary stroke,
say nothing about the alias) while quietly breaking the invariant the
parent feature already committed to.
*Recommendation:* Add an explicit AC for how aliased/multi-stroke ops
(DELETE, REDO) behave under rebind and reset.

**5. [MEDIUM] AC5's verification ("a test asserts bindings resolve through one path") is vague enough to be gamed.**
Given finding #1, there is today no single path to assert against — the
canvas InputMap and `EditOp.accelerator()` are independently maintained.
Absent a concrete definition (e.g., "every `EditOp`'s canvas `InputMap`
entry is derived by calling `EditOp.accelerator(osName)`, verified by a
reflective/parameterized test over all 18 constants"), an implementer could
satisfy the letter of AC5 by adding a thin `AcceleratorResolver` that only
some call sites (e.g. the new settings dialog) go through, while the
pre-existing hand-written canvas bindings from finding #1 remain
unconverted and untested by the new "one path" assertion.
*Recommendation:* Name the concrete invariant AC5's test must check (e.g.
"no `KeyStroke.getKeyStroke` literal for an `EditOp` binding exists outside
`EditOp.accelerator()`", checkable by a source-scan test in the style of
`KeyPadAccessibilityPinTest`).

**6. [LOW-MEDIUM] Sizing risk.** `band_mw: 1.5-2` prices this as "expose an
existing model," but per findings 1-2 it also needs: a net-new settings
dialog (no comparable settings/preferences UI exists in `src/jls` today —
grep for `JDialog`/`Settings`/`Preferences` under `src/jls` turns up only
`About.java`, `KeyPad.java`, `Tutorial.java`, and `UserPrefs.java`'s own
`java.util.prefs` import), a rebind-capture UX, scope-aware conflict
detection (finding 3), and a persisted structure `UserPrefs` doesn't yet
have (today it stores four flat scalar keys — theme, two colors, undo
depth — never a variable-length map of op→keystroke overrides). This looks
underscoped once the "already unified" premise is corrected.

**7. [LOW] Readiness flag, not a defect in this issue.** The machine block's
`ordering_after: [TASK-C570-1]` correctly sequences this behind #787 ("each
Digital-wishlist item gets its D10 path-and-cost justification in writing
before any of it is implemented" — itself required by #570's AC-4). #787 is
still open, so #789 is not yet unblocked to start regardless of how sound
its own acceptance criteria are.

## What's solid

- **AC2's persistence contract is grounded in working precedent.** `UserPrefs`
  (`src/jls/UserPrefs.java`) already implements exactly the "corrupt/missing
  falls back to default, never throws" pattern this AC asks for —
  `parseColor`/`parseUndoDepth` (lines 230-263) catch `NumberFormatException`
  and return the current default. Extending this store, rather than
  inventing a new mechanism, is the right call.
- **AC4's regression guard already exists and is on-point.**
  `test/jls/HotkeysHelpAccuracyTest.java` cross-checks `EditOp.accelerator()`
  against `hotkeys.html` today; requiring it to keep passing against the
  reset default set is a real, automatable, non-gameable check.
- **Citing the #75 2026-07-19 hold as the shadowing precedent is accurate**
  (verified against #75's body) and correctly motivates AC3's "refused at
  entry" requirement in spirit, even though the mechanism (finding #3)
  needs more precision.
