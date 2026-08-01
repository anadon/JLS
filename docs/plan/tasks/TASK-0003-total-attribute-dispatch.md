# TASK-0003 - Make attribute dispatch total and the loader check it

**Status:** proposed | **Cost:** 1 wk | **Blocked by:** none

## Deliverable

`Element.setValue` reports an unmatched attribute name instead of returning
silently, and the loader's five call sites turn that report into a diagnostic
naming file, line, element and attribute.

1. **The five overloads become total.**
   `src/jls/elem/Element.java:344-397` - `setValue(String,int)`,
   `setValue(String,long)`, `setValue(String,BigInteger)`,
   `setValue(String,String)` each iterate `savedAttributes()` and `return`
   after the loop with no signal. Change each to return `boolean`: `true` when
   an attribute consumed the name, `false` otherwise.
2. **Every override is updated.** 26 `public void setValue` declarations across
   17 classes in `src/jls/elem/` (`Element`, `LogicElement`, `Group`, `Pin`,
   `Input`, `Output`, `WireEnd`, `WireNet`, `Memory`, `Extend`, `FieldExtend`,
   `Constant`, `JumpEnd`, `State`, `StateMachine`, `SubCircuit`, `TruthTable`).
   Each override that handles its own names and then delegates - the shape at
   `src/jls/elem/WireEnd.java:625-640`, which handles `attach`, `wire` and
   `tristate` outside `savedAttributes()` and calls `super.setValue` in the
   `else` - returns `true` for its own names and the super result otherwise.
3. **The loader consumes the result.** `src/jls/Circuit.java:1067, 1078, 1089,
   1105, 1116` (the `int`, `long`, `Int`, `String` and `ref` arms of the
   element-body reader) call `el.setValue(name, value)` unconditionally and
   discard the outcome. Each becomes: on `false`, record an
   **unconsumed-attribute diagnostic** carrying the element tag, the attribute
   name, the item kind and `lineNumber` (the loader's running counter,
   incremented at each of those sites).
4. **The diagnostic is a report, not a refusal.** `docs/file-format.md:220-228`
   is normative: *"Unknown attribute names are silently ignored ... the
   format's main forward-compatibility valve"*. The load must still succeed.
   Add `LoadError.Category.UNCONSUMED_ATTRIBUTE`
   (`src/jls/LoadError.java:38-90`) and a non-fatal channel: a
   `List<LoadError>` of warnings on the load result, surfaced by the batch CLI
   on stderr and by the GUI in the existing load-error path, rather than routed
   through `failLoad` (`src/jls/Circuit.java:802, 820`), which returns `false`
   and aborts.
5. **`docs/file-format.md` §5 gains one sentence** recording that the reference
   implementation now *reports* the drop it is permitted to make silently. The
   normative rule does not change and no `FORMAT` bump is involved.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-002 | This is the fail-loud half of the feature: the loader stops accepting data it cannot store. |

## Prerequisite tasks

None. TASK-0004 supplies the corpus that proves it, and depends on this, not
the reverse.

## Acceptance test

`test/jls/LoadErrorReportingTest.java` (existing) gains:

- `unconsumedIntAttributeIsReportedWithFileLineElementAndName()` - loads a
  circuit text whose `AndGate` block carries `int notAnAttribute 7`, asserts the
  load **succeeds**, that exactly one warning is present, that its category is
  `UNCONSUMED_ATTRIBUTE`, and that its detail contains `AndGate`,
  `notAnAttribute` and the correct 1-based line number.
- `unconsumedAttributeOnEveryItemKindIsReported()` - the same for `long`, `Int`,
  `String` and `ref` items, one per kind, proving all five call sites were
  converted.
- `wireEndHandwrittenNamesAreStillConsumed()` - a `WireEnd` block with `ref
  attach`, `ref wire` and `int tristate` produces **zero** warnings, proving
  the hand-written override path reports `true`.

## Related GitHub issues

**No issue.** The silent-data-loss path has no tracker entry: neither
`Element.setValue` nor the loader's discard of its outcome is filed. The
registry records this gap deliberately (FEAT-002, TASK-0003, TASK-0004).

Adjacent but not the same: **#78** (element descriptor and registry) would make
attribute declarations self-describing, which would make this check derivable
rather than hand-added; it does not itself close the silent drop.

## Notes

- **The central trap is the spec.** `docs/file-format.md:220-228` and §9's
  "silent-drop caveat" (`:466-480`) make silent ignoring of unknown attribute
  names a *load-bearing forward-compatibility guarantee*, with `Memory`'s
  `initrle` and `sync` as the recorded live instances. Converting the drop into
  a refusal would break every file written by a newer JLS. The deliverable is a
  **diagnostic**, and any implementation that fails the load is wrong.
- **`setValue` is public API on a sealed hierarchy.** Changing `void` to
  `boolean` is a source-incompatible signature change at all 26 sites; the
  compiler finds every one, which is the point. There is no out-of-tree caller:
  `Element` is `sealed ... permits DisplayElement, LogicElement, Wire`
  (`src/jls/elem/Element.java:20-21`).
- **`setPair` and the `probe` item are separate arms** (`src/jls/Circuit.java`
  `pair` and `probe` branches) and take a different path -
  `el.setPair(v1,v2)` has no name at all. Do not fold them in; record in the
  file-format doc that the pair item is nameless and so cannot be unconsumed.
- **The `id` special case runs before the call**: `src/jls/Circuit.java:1065-1066`
  puts `id` into `elementMap` and then still calls `setValue`. `id` is a base
  attribute (`docs/file-format.md:214`) so it is consumed; do not treat the
  pre-registration as consumption.
- **NullAway** is enforced in `@NullMarked` packages (`CONTRIBUTING.md`); the
  new warning list must be non-null and handed out as an unmodifiable copy, per
  the value-semantics rule (#94).
- **`LoadError.Category` is an enum switched over elsewhere**; check every
  switch on it before adding `UNCONSUMED_ATTRIBUTE`, and do not add a `default:`
  arm.

## Evidence

- `src/jls/elem/Element.java:344-397` - the four attribute-loop overloads, each
  falling through with no signal.
- `src/jls/Circuit.java:1067, 1078, 1089, 1105, 1116` - the five discarding call
  sites, one per item kind.
- `src/jls/elem/WireEnd.java:625-640` - the override shape that handles names
  outside `savedAttributes()`.
- `grep -rn "public void setValue" src/jls/` - 26 declarations across 17 files.
- `docs/file-format.md:220-228` (§5, silent-ignore rule), `:466-480` (§9
  silent-drop caveat, `initrle` and `sync`).
- `src/jls/LoadError.java:31, 38-90` - the record and its category enum.
