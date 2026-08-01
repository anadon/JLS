# TASK-0019 - The editor decomposition plan and its coverage floor

**Status:** proposed | **Cost:** 1.5 wk | **Blocked by:** none

## Deliverable

A written extraction plan measured against HEAD, and a `jls.edit` coverage
floor that turns the plan into a ratchet instead of an intention.

1. **Re-measure, because issue #84's numbers are stale in the wrong
   direction.** #84's title says `SimpleEditor.java` is 4,119 lines with a
   "305-line `source==` dispatcher". At HEAD the file is **5,852 lines** and
   the dispatcher is down to **one** residual site — see TASK-0020. The plan
   opens with the current census: line counts for every class in
   `src/jls/edit/` (84 files, 23,910 lines total), what has already been
   extracted (`UndoManager.java` 239, `Palette.java` 264, `EditOp.java` 155,
   `ElementDialogs`, `ElementRenderers`, `Viewport`, `KeyboardConstructionPolicy`,
   `DeleteKeyPolicy`, `OptionMenuPolicy`, `SwingTextMetrics`), and what has
   not.

2. **The plan document, `docs/editor-decomposition-plan.md`.** One row per
   class still to extract, each carrying: the extraction's name; the exact
   HEAD line ranges it moves; what it depends on that is *not* AWT; what it
   depends on that *is*, and how that dependency is inverted; the test surface
   that pins it before the move; and its order relative to the others. The
   known remaining bodies are the interaction state machine (TASK-0020), the
   toolbar/menu construction that survives `#78`'s registry
   (`SimpleEditor.java:2306-2354`), clipboard and import management, and the
   `EditWindow` inner class itself (`:1121`), which is a non-static inner class
   closing over the outer editor and is the reason the state split exists.
   Each row states its own target line count, so "did the extraction land"
   is a number and not an opinion.

3. **A `jls.edit` package floor in the coverage ratchet.** `pom.xml:408-411`
   records that "`jls.edit` is deliberately unfloored until the #91/#84 work
   makes editor code testable", and `CONTRIBUTING.md:106-108` restates it with
   the reason: a floor added earlier would "either bind at ~0% or block
   unrelated PRs". `test/jls/ui/` now holds 34 files and 25 `@Tag("display")`
   classes, so the premise has changed. Add a `PACKAGE` rule for `jls.edit`
   to the `coverage-ratchet` execution (`pom.xml:347-518`) at the measured
   value less the documented epsilon, and edit both comments from "deliberately
   unfloored" to the raise-only rule that governs every other package.

4. **The measurement basis is stated in the pom comment.** This is the trap
   (below): the number must say which surefire execution produced it.

5. **A floor-inventory ratchet.** A test asserting that every package under
   `src/` either has a `PACKAGE` rule in the `coverage-ratchet` execution or
   appears in a declared exemption list with a reason — so the next unfloored
   package is a deliberate, documented choice rather than an omission. There
   are 18 packages at HEAD (`jls`, `jls.boot`, `jls.collab.crdt`,
   `jls.collab.net`, `jls.collab.op`, `jls.collab.session`, `jls.core`,
   `jls.edit`, `jls.elem`, `jls.hdl`, `jls.hdl.board`, `jls.hdl.imp`,
   `jls.hdl.layout`, `jls.hdl.scan`, `jls.hdl.yosys`, `jls.module`, `jls.sim`,
   `jls.util`) and four rules.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-008 | "`jls.edit` carries a coverage floor" is a third of the feature's title. Without it no extraction can be shown to have preserved coverage, and the feature's cost band cannot be checked against progress. |

## Prerequisite tasks

None. The UI substrate the floor is measured against already exists at HEAD.

## Acceptance test

`test/jls/CoverageFloorInventoryTest.java`, new (the `*RatchetTest`/inventory
family in `test/jls/`):

- `everyProductionPackageHasAFloorOrADeclaredExemption()` — enumerates the
  packages by walking `src/**/package-info.java`, parses the `PACKAGE` rules
  out of `pom.xml`'s `coverage-ratchet` execution, and asserts each package is
  in one set or the other. **Fails at HEAD** on `jls.edit` and thirteen
  others; the fix is the `jls.edit` rule plus an exemption list that names the
  rest with reasons, so the failure converts an implicit gap into a written
  decision.
- `theExemptionListNamesAReasonForEveryEntry()` — asserts no bare package name
  in the exemption list.

The floor itself is proven by `mvn clean verify` going green with the new rule
and by a deliberate coverage-lowering edit turning it red. Record both in the
PR, as `CONTRIBUTING.md:103-105` requires: "a floor that has never been seen to
fail should be assumed vacuous".

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 84 | Decompose `SimpleEditor`: 4,119 lines, a 9-state mouse machine, a 305-line `source==` dispatcher that already caused #37, and whole-circuit undo snapshots | depends on — this task writes #84's §7 method as a costed plan against re-measured numbers, and supplies the safety net #84's §4 H1 assumes; TASK-0020 does the first extraction |
| 91 | Automated UI test harness: assert element presence, geometry, relations, actions, menus, and mouse interactions | depends on — #91's harness is the instrument the floor measures; TASK-0021 owns its residual |
| 162 | UI-layer coverage: a CI display substrate for #91 layers 2-3, dialog-construction coverage for all 23 element dialogs, and interactive-simulator smoke | depends on — the display substrate is what makes a non-vacuous `jls.edit` floor possible at all |
| 78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs — its registry half already shipped and already retired the hand-enumerated toolbar factory #84 named; the plan must not re-plan that |

Recorded decisions, closed, cite as such: **#37** (the dead-popup bug the
dispatcher pattern produced)
and **#159** (the coverage ratchet). **PR #233** (the zero-margin floor flake)
and **PR #244** (the 0.50 headless LINE milestone) are **pull requests, not
issues**; cite them as such.

## Notes

- **The floor's measurement basis is the trap, and it is the whole task.**
  `CONTRIBUTING.md:95-99` says floors are raised from **headless** numbers
  only. But most of `jls.edit` is exercised by the `display-tests` surefire
  execution under Xvfb (`pom.xml:274-295`), and the JaCoCo agent appends to a
  single `target/jacoco.exec` (`CONTRIBUTING.md:100-102`), so a run that
  includes the display suite unions its coverage in. A floor set from the
  CI-style `xvfb-run -a mvn -B verify -Djls.test.headless=false`
  (`.github/workflows/ci.yml:80-86`) is therefore **unmeetable by a plain
  local `mvn verify`**. Decide out loud which of the two is the basis, write
  it in the pom comment, and — if the answer is "the display run" — say what
  a contributor without a display is expected to do. The recommendation is to
  floor `jls.edit` against the headless-only number, which is lower and
  honest, and to raise it as headless-tractable extractions land.
- **Leave the documented epsilon.** `CONTRIBUTING.md:83-89`: a
  `jls.collab.op` BRANCH floor of 0.770 set from JDK 25 measured 0.768 on
  JDK 26 and turned the build red for no code reason (PR #233). BRANCH jitters
  most.
- **`include` patterns must be dot-form.** `CONTRIBUTING.md:104-105`: the
  slash form silently matches nothing, so a wrong `jls/edit` rule would look
  like a passing floor forever.
- **The plan must not re-plan what shipped.** `UndoManager` is extracted
  (`src/jls/edit/UndoManager.java`, 239 lines, referenced from
  `SimpleEditor.java:155-156`), the palette is registry-driven
  (`Palette.java`, `makeElements` now 48 lines at `:2306-2354`), and the
  shared-`Action` layer landed (`:605-608`, `:1304`, `:1594`). #84's §7
  checklist is partly done; the plan states which boxes are ticked and by
  which commit.
- **`SimpleEditor.java` grew from 4,119 to 5,852 lines while parts were being
  extracted.** That is the number to put at the top of the plan, because it is
  the argument for the floor: extraction without a ratchet has not been net
  reducing.
- **This is a planning task inside a planning corpus, and the distinction
  matters.** The deliverable is a document *in the repository* under
  `docs/`, plus a pom change and a test — not a document in `docs/plan/`.

## Evidence

- `src/jls/edit/SimpleEditor.java` — 5,852 lines at HEAD (`wc -l`); the
  package totals 23,910 lines over 84 files.
- Already extracted: `src/jls/edit/UndoManager.java` (239),
  `Palette.java` (264), `EditOp.java` (155), plus `ElementDialogs`,
  `ElementRenderers`, `Viewport`, `KeyboardConstructionPolicy`,
  `DeleteKeyPolicy`, `OptionMenuPolicy`, `SwingTextMetrics`.
- `src/jls/edit/SimpleEditor.java:155-156` (the `UndoManager` field),
  `:2306-2354` (`makeElements`, now registry-driven), `:605-608` and `:1594`
  (`editAction`), `:1121` (the `private class EditWindow`).
- `pom.xml:347-518` — the `coverage-ratchet` execution: the bundle rule and
  four `PACKAGE` rules (`jls` 0.515/0.500/0.555, `jls.sim` 0.930/0.920/0.845,
  `jls.elem` 0.730/0.700/0.585, `jls.collab.op` 0.905/0.895/0.750).
- `pom.xml:408-411` — "jls.edit is deliberately unfloored until the #91/#84
  work makes editor code testable".
- `pom.xml:261-296` — the two surefire executions that decide what the floor
  can see.
- `CONTRIBUTING.md:69-125` — the ratchet rules: raise with your PR, epsilon
  headroom, canonical JDK only, headless only, `mvn clean verify`, vacuous
  floors, dot-form includes, and the `jls.edit` exemption at `:106-108`.
- `.github/workflows/ci.yml:80-86` — how CI actually runs the suite
  (`xvfb-run -a mvn -B verify -Djls.test.headless=false` when Xvfb is
  present, plain `mvn -B verify` otherwise).
- 18 packages under `src/` at HEAD, by `package-info.java`.
