# Stable Component Naming Scheme (issue #210)

UI components that automation (the #91 harness) or assistive technology
needs to resolve carry a stable Swing component name (`setName`). Names
are the lookup contract: tests and tools resolve components with
`getName()` matching instead of tooltip text, Swing class, or positional
index. This document is the scheme's source of truth; the
`jls.ui.ComponentIdentityTest` display test enforces it on the live app.

## Element slugs

Wherever a name refers to an element type, the *slug* is the element's
`jls.elem.ElementRegistry` tag (issue #78's element table), lower-cased:
`Adder` → `adder`, `AndGate` → `andgate`, `ShiftRegister` →
`shiftregister`. Sourcing slugs from the registry keeps the names in
sync with the element table; `SimpleEditor.paletteSlug` refuses to build
the tool bar if a palette entry does not map to a registered tag.

## Palette (tool bar) and mirror menu

| Component | Name |
| --- | --- |
| Tool-bar palette button | `palette.<slug>` (e.g. `palette.adder`) |
| Mirror item in the popup "elements" menu | `menu.elements.<slug>` |

Both are assigned in `SimpleEditor.makeElement`. All 30 palette entries
are named; the accessible *name* (the human-readable element label from
the tooltip, added by #75) is unchanged.

## Element create/modify dialogs

Element dialogs extend `jls.edit.ElementFormDialog`, which owns the
shared identity:

| Component | Name |
| --- | --- |
| The OK button (every element dialog) | `dialog.ok` |
| The Cancel button (every element dialog) | `dialog.cancel` |
| A labelled form input | `dialog.<slug>.<field>` (e.g. `dialog.adder.bits`) |

Form inputs are wired with the `ElementFormDialog.labelled(label, field,
name)` helper, which in one call:

- sets `JLabel.setLabelFor(field)` so a screen reader announces the
  label when the field gains focus,
- sets the field's component name for name-based lookup, and
- sets the field's accessible name to the label text (minus any
  trailing colon).

`<field>` is a short camelCase description of the parameter
(`bits`, `name`, `capacity`, `cycleTime`, `initialValue`, ...).
Dialogs shared by several element types use the dialog's own slug
rather than one element's tag: `dialog.gate.*` (the shared AND/OR/NAND/
NOR/NOT/XOR/Extend form), `dialog.group.*` (Splitter/Binder),
`dialog.pin.*` (input and output pins), `dialog.delaychange.*` (the
change-timing form). Sub-forms extend the prefix with a segment, e.g.
`dialog.statemachine.state.name` and
`dialog.statemachine.transition.signal`.

Unlabelled inputs (the SigGen and Text free-text areas, and the
Memory built-in initial-contents area raised from the memory form) have
no `labelFor` source, so they receive `setName` plus an explicit
accessible name directly: `dialog.siggen.signals`, `dialog.text.text`,
`dialog.memory.contents`.

## Adding a new element

1. Register the element in `ElementRegistry` (enforced by
   `ElementRegistryTest`).
2. `SimpleEditor.paletteSlug`: add the palette tooltip → tag case (the
   tool bar fails fast at construction if you forget).
3. In the element's dialog, wire every labelled input through
   `labelled(...)` with a `dialog.<slug>.<field>` name.
