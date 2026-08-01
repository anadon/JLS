# FEAT-008 - `SimpleEditor` decomposition, a UI harness and a floored `jls.edit`

**Status:** proposed | **Cost:** 12-20 mw | **Owner program:** UNOWNED |
**Spine rank:** -

## Capability delivered

The editor becomes testable without a human: its nine-state mouse machine is a
class with transitions that do not depend on drawing, a harness asserts element
presence, geometry, relations, actions, menus and mouse interaction, every
element dialog has construction and validation coverage, and `jls.edit` carries
a coverage floor on the raise-only ratchet like every other package. That is
what makes it possible to add a second canvas - breadboard, analog, PCB - at all,
because today the package is the only one in the tree exempted from the coverage
bar and a new canvas would spend the entire bundle headroom.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-00 | required | The largest single class in the tree, an unfloored package, and three open issues that have sat for a decade |
| CAP-01 | required | Collaborative editing replays ops into the editor; an untested editor makes convergence unassertable |
| CAP-04 | required | The breadboard canvas is a second canvas inside a coverage budget that does not exist yet |
| CAP-10 | beneficial | An audio-bearing circuit needs its palette and its panes, which is editor surface |
| CAP-11 | beneficial | Same |
| CAP-12 | beneficial | The heart-rate demo is a mixed analog/digital canvas with a per-view palette |
| CAP-16 | beneficial | A migration target a Logisim user can actually drive is an accessibility and editor claim |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-015 | The op layer is the mutation seam the decomposition extracts *to*: a gesture that ends in `OpSink.submit` is testable headlessly, and one that mutates inline is not |
| FEAT-007 | Editor tests need a display substrate in CI; without the GUI lane the harness is green only on developer machines |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0019 | The editor decomposition plan and its coverage floor | Names each class to extract, its dependencies and its test surface, and sets the initial floor at the measured value |
| TASK-0020 | Extract the mouse machine and replace the source-identity dispatcher | The nine-state machine becomes a tested class with no drawing dependency in its transitions |
| TASK-0021 | The UI test harness, including dialog construction | Shared with FEAT-053: the harness that asserts presence, geometry, relations, actions, menus and mouse interaction |
| TASK-0018 | Wayland GUI rig first light | Shared with FEAT-007: the CI display substrate the harness runs on |
| TASK-0069 | Transcript capture, replay and the console pane | Shared with FEAT-032 and FEAT-034: the console pane binds through the existing runner/event-thread seam and is the first new pane the decomposition must accommodate |
| TASK-0105 | Per-view palettes and the analog palette | Shared with FEAT-049, FEAT-043 and FEAT-029: the palette contract gains a view dimension, which a currently-green test forbids |

## Acceptance criteria

1. A written decomposition plan names each class to extract, its dependencies
   and its test surface, and is committed before extraction begins.
2. The mouse state machine is a class whose transitions contain no drawing calls
   and are exercised by unit tests over the full nine-state vocabulary.
3. `jls.edit` has a JaCoCo rule in `pom.xml` with a nonzero minimum, set at the
   value TASK-0019 measures, with at least a point of headroom, and it only ever
   rises.
4. The harness asserts element presence, geometry, relations, actions, menus and
   mouse interaction without a human, and runs in the CI display lane.
5. Every element dialog has construction and validation coverage. The count is
   read from the registry, not hard-coded, so a new element type cannot arrive
   without its dialog test - the FEAT-001 discipline applied to the editor.
6. Adding a second canvas does not require raising the bundle floor, because the
   package floor now absorbs it.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 84 | Decompose `SimpleEditor`: 4,119 lines, a 9-state mouse machine, a 305-line `source==` dispatcher that already caused #37, and whole-circuit undo snapshots | closes - but see the design note: the class is 5,852 lines at HEAD and the `source ==` dispatcher is largely gone |
| 91 | Automated UI test harness: assert element presence, geometry, relations, actions, menus, and mouse interactions | closes |
| 162 | UI-layer coverage: a CI display substrate, dialog-construction coverage for all 23 element dialogs, and interactive-simulator smoke | closes - the dialog count in the title is stale against a 35-type registry |
| 101 | Wayland GUI rig: boot the GUI on JBR's WLToolkit under headless sway in CI, screenshot it, and publish first-light findings | overlaps - shared with FEAT-007 via TASK-0018 |

## Design notes

**Re-measure before quoting the issues.** `src/jls/edit/SimpleEditor.java` is
**5,852 lines** at HEAD, not the 4,119 in #84's title - it has grown by about
1,733 lines since the issue was filed, which is the strongest available argument
for the decomposition and should be stated as such. Conversely, part of #84 has
already been done: the 305-line source-identity dispatcher is down to a single
`source ==` occurrence in the file, so TASK-0020's dispatcher half is smaller
than the issue implies while its mouse-machine half is unchanged. The `State`
enum with its nine constants is at `src/jls/edit/SimpleEditor.java:770-789`.
#162's "23 element dialogs" is stale against a 35-type registry
(`src/jls/elem/ElementRegistry.java:38-77`); criterion 5 derives the count
rather than restating it.

The exemption is explicit and documented, which makes it a decision to reverse
rather than an oversight to fix: `pom.xml:408-409` reads "jls.edit is
deliberately unfloored until the #91/#84 work makes editor code testable", with
the same raise-with-your-PR convention as the bundle floors. That sentence names
this feature as its own precondition, so landing it is what discharges the note.

The op layer is further along than the plan elsewhere assumes and the extraction
must build on it rather than around it. `src/jls/collab/op/` holds 21 files
including `CircuitOp`, `OpSink`, `CircuitOpReader`, `Ops` and eighteen concrete
ops, `docs/operation-layer.md` is the written contract, and `OpSink.submitAll`
(`src/jls/collab/op/OpSink.java:42`) already collapses a multi-op gesture into
one undo snapshot. The remaining coupling is `CircuitOp.apply(Circuit, Graphics)`
(`src/jls/collab/op/CircuitOp.java:51`), which FEAT-015 replaces with a text
metrics abstraction - and `Circuit.finishLoad` already takes
`jls.core.TextMetrics` (`src/jls/Circuit.java:1300`), so the pattern exists.

Undo remains whole-circuit snapshotting (`CircuitSnapshot`; `markChanged()`
occurs 16 times in the file, of which 13 are mutation-marking call sites, one is
an anonymous-adapter override at `:5497` and one is a comment), and the measured
per-edit cost is 58 ms at 10,000 elements and 552 ms at 100,000. Replacing it is
*not* in this feature's scope; making it replaceable is.

## Risks

- **12-20 mw and UNOWNED is the largest unfunded item in the maintenance
  column.** It is also the one whose absence is compounding - the class grew 42%
  since the issue was filed. The plan should say plainly that deferring this
  raises its own cost.
- **A coverage floor set before decomposition can be set too high to move.**
  TASK-0019 sets it at the *measured* value with headroom, per the PR #233 lesson
  recorded at `pom.xml:400-408`; setting it aspirationally produces a red build
  nobody can green.
- **GUI tests are the classic flake source.** They are also the only way to
  assert criterion 4. Mitigation is that the harness asserts model state through
  the op layer wherever possible and pixels only where nothing else will do.
- **Bus factor 1 on a five-month refactor of the file everything touches.** The
  decomposition plan being written and committed *first* (criterion 1) is what
  makes the work resumable by someone else.

## Evidence

- Size at HEAD: `wc -l src/jls/edit/SimpleEditor.java` = 5,852 (issue #84 says
  4,119).
- The nine-state machine: `src/jls/edit/SimpleEditor.java:766-789`.
- The source-identity dispatcher is largely gone: one `source ==` occurrence
  remains in the file at HEAD.
- The explicit exemption and the raise-with-your-PR convention:
  `pom.xml:400-418`, especially `:408-409`.
- Op layer at HEAD: `src/jls/collab/op/` (21 files), `CircuitOp.java:51`
  (`apply(Circuit, Graphics)`), `OpSink.java:24,42`, `docs/operation-layer.md`.
- Undo snapshotting: `src/jls/edit/CircuitSnapshot.java`; the 13 mutation-marking
  call sites at `src/jls/edit/SimpleEditor.java:749,1402,2560,2819,2873,2929,
  3439,4964,5105,5244,5552,5569,5754`, plus the adapter override at `:5497-5500`.
- Per-edit cost 58 ms at 10k elements, 552 ms at 100k: `BRIEF.md` §7.
- Element type count for criterion 5: `src/jls/elem/ElementRegistry.java:38-77`.
- Governance bar this work must meet: `CONTRIBUTING.md`; 93.0/92.0/84.5 JaCoCo
  package aggregate plus 80/82 PIT (`BRIEF.md` §8).
- **Cost reconciliation.** Band 12-20 mw. Tasks named for it: TASK-0018,
  TASK-0019, TASK-0020, TASK-0021, TASK-0069, TASK-0105, totalling 10.5 wk.
  The named tasks are the leading, dividable slices of this feature, not the
  whole of it; the residual has no task id, because the registry's task space
  is closed at TASK-0112. Do not read 10.5 wk as the feature. Shared tasks
  counted once at the task level: TASK-0018, TASK-0021, TASK-0069, TASK-0105.
