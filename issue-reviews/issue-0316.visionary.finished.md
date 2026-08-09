# Issue #316: FEAT-008 (RESIDUAL): the editor's nine-state mouse machine is assertable without a display, and jls.edit carries a coverage floor
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is actually for

Strip the roster and the mermaid and one sentence remains: *editor changes should
stop being unverifiable, so a second canvas can be added without fear.* That end is
right and is squarely on the project's arc — `docs/grand-architecture.md` §8 names
#84 "the gui-module counterpart to #77," and ARCHITECTURE.md's own editor section
concedes `SimpleEditor` is where "most editor behavior is here."

I endorse the end. I do not endorse the means: the issue picks a GoF state
decomposition as the seam, a package-average coverage percentage as the instrument,
and a five-deliverable bundle as the unit of scheduling. All three are, on this
repo's own evidence, worse than alternatives the repo has *already built and proven
elsewhere*.

## 1. The bundle is itself the defect — dissolve the gate

#316 packs a five-month refactor, a one-line CI digest, a test-tree sweep, and a
data-model widening into one blocking node. The harm is not hypothetical; it is
recorded in this issue's own third comment: #571's contributor outreach inherited a
12–20 mw editor refactor as a prerequisite and had to be narrowed to AC-2/AC-5, and
#524/#686/#757 had inherited it transitively through #369. The maintainer's fix —
"name the criterion, not the issue" — treats the symptom. The cause is that a
feature-tier issue exists at all around five things whose only common property is the
string `jls.edit`.

**Reframing:** #316 stops being an ordering node and becomes a label. Nothing carries
`blocked_by: [316]`; downstream edges point at the specific child (#440, #84, #470,
#411, #482). §6 already admits three of six children are "mutually independent, safe
for separate agents" — the machine block simply contradicts §6. Concretely, TASK-0018
(`.github/workflows/ci.yml:380`, `JBR_SHA256:
"UNVERIFIED-PLACEHOLDER-..."`) is a live unpinned-toolchain download sitting behind a
months-scale editor refactor. That is not a scheduling nuisance; it should land this
week, on its own, with no relationship to the mouse machine.

## 2. The seam is wrong: plans, not state objects

§3 "Provides" specifies nine state classes plus "an explicit context object exposing
the gesture fields the branches currently reach directly (selection, wire-end/wire/net,
caret, `OpSink` submission)." Read that against the code: `currentState` is not on
`SimpleEditor` at all — it lives on the inner `EditWindow` (`SimpleEditor.java:1218`)
alongside `selected`, `selRect`, `moveOriginX/Y`, `viewport`, and the `message` label,
and the branches call `viewport.toModel`, `circuit.elementsAt`, `el.setHighlight`,
`repaint()`, `optionMenu.show(this,sx,sy)` and `message.setBackground` (see the idle
branch at `:2609-2695` and `setState` at `:3936-3975`). The proposed "context object"
*is* `EditWindow` with a new name. Nine classes plus one god-context leaves coupling
exactly where it was — it just becomes an interface, and §7's own top refute risk ("a
hidden `Component` dependency") is the predictable outcome rather than a surprise.

**The alternative is already in the tree and already working.**
`SimpleEditor.deleteSelectionPlan` (`:872`) and `moveSelectionPlan` (`:1053`) are
`static`, Swing-free functions returning `List<CircuitOp>`, tested headlessly by
`test/jls/edit/DeleteGestureTest.java` and `MoveGestureTest.java` — no display tag, no
harness, no state objects. `docs/operation-layer.md` ("What lands next") names
preview-then-commit for the remaining gestures as the next step, with a per-gesture
inventory already written. That is the seam that is paying off here empirically.

Cut along it: an interaction step is a pure function
`(state, event, hit-test result) → (next state, op plan, feedback record)`. Swing
becomes an adapter that applies the plan through `OpSink` and renders the feedback.
Under that framing the transition table is *data* — a total function over the nine
constants, assertable in an afternoon with no context object — and the actual weight
(migrating inline mutation) is #167/#282/#283/#337's scope, already owned, already
funded. FEAT-008's TASK-0020 shrinks from a months-scale refactor to a thin cap on
work in flight. That is the reframing that makes most of this problem disappear.

It also resolves, decisively, the design collision the second comment escalated here:
GoF state objects (§3) versus a behaviourless enum plus a `MouseMachine` returning a
`Transition` record (#441, absorbed into #84). The plan framing is the second, and it
is the one that keeps the AWT-free invariant (§4.6) reachable, because plans and
feedback records are data and can live in an AWT-free package the way `jls.collab.op`
does under `ArchitectureRulesTest.collabLayersAreHeadless`.

## 3. The instrument is wrong: a package average over 84 files measures the wrong thing

`src/jls/edit/` is 84 Java files, of which 29 are `*Renderer` and 28 are `*Dialog*`.
A single line/branch floor over that mix is satisfiable in full without touching one
line of the mouse handlers — cheap renderer smoke tests and dialog construction
sweeps move the number; the interaction machine does not have to. The repo already
records the failure mode of percentage floors (`pom.xml:400-418`, the #233 finding
that zero-margin floors flake across the JDK matrix), and already trusts a better
instrument: `HeadlessCoreRatchetTest`, `ArchitectureRulesTest`, `PointerApiRatchetTest`,
`NotificationRatchetTest`, `PackageInfoRatchetTest`.

**Reframing:** put the extracted interaction layer in its own package
(`jls.edit.interact`), floor *that*, and add a structural ratchet in the established
family — no `Graphics`/`repaint`/`JOptionPane`/Swing type reachable from the
interaction package, and every row of `docs/operation-layer.md`'s mutation-site
inventory has a `*Plan` method with a headless test. That measures the property the
issue actually wants, cannot be gamed by dialog sweeps, does not flake across JDKs,
and removes TASK-0019's own stated refutation risk (that the measured floor is
effectively zero — of course it is, when the target code is untestable by
construction).

This also repairs §1's second-canvas claim, which is misdescribed as written. A
package-average floor does not "absorb" a large new untested canvas; it *dilutes*,
and under a raise-only convention dilution is a landing hazard, not headroom. A
per-package floor with the new canvas in its own package makes the claim true by
construction. Separately: #329's breadboard is a different interaction model (parts
seated in a grid; no wire-drawing gesture at all). What a second canvas shares with
the schematic is the *op vocabulary* (#337), not a nine-state machine tuned to JLS's
wire gestures. Forcing it through the shared machine is the pull-against-the-arc risk
in this issue.

## 4. Dialog validation was already invented here, in a better form

§5 criterion 4 asks to "open each element dialog, enter an invalid value, and commit."
`test/jls/elem/DialogValidationTest.java` already exists and states the superior
contract in its own header: every rule is "stated exactly once — a shared
helper/constant on the element — that both the dialog and the file loader reject with
(P5: one string, two surfaces, never two wordings)," and, explicitly, "dialogs need a
display, so these tests exercise the shared rule helpers the dialogs call." #316's
plan regresses that into the display lane. The genuine residual is *totality*: ten
tests covering a handful of types, against a 35-type registry.

**Reframing:** derive it the way this repo already derives totality —
`ElementRegistryTest` and `PaletteContractTest` are the models. One registry-driven
test over `ElementRegistry.ALL` × declared attributes, feeding an out-of-domain value
and asserting the shared constraint string on both surfaces. That satisfies criterion
3 ("a 36th type with no dialog test goes red") by construction, stays headless, and
puts the rule where the rest of the trajectory needs it — #337's programmatic verbs
and the batch/grading surface must enforce the same constraints with no dialog in
sight. Writing validation into 35 Swing dialogs builds the rule in the one layer the
architecture is trying to leave.

## Acceptance criteria I am explicitly disregarding

- **`git grep -c "switch (currentState)"` returns 0.** This is text-matching a
  syntactic form, and under the reframing above it forbids the strongest tool Java
  offers for this exact job: an exhaustive `switch` *expression* over the state enum,
  which turns a tenth state into a compile error. Delete the criterion; replace it
  with "no interaction transition reaches a Swing type," which the ratchet can check.
- **"an explicit context object exposing … selection, wire-end/wire/net, caret,
  `OpSink` submission"** (§3 Provides). This is `EditWindow` renamed. Replace with:
  transitions take an immutable hit-test/selection snapshot and return a plan.
- **The `jls.edit` package-wide floor as the deliverable of TASK-0019.** Keep the
  measurement; change the shape to a scoped floor plus a structural ratchet.

## What I would keep verbatim

§4's invariants are excellent and rare: byte-identical saves, "a test whose asserted
behaviour must change refutes the step; it is not edited to match," no floor lowered,
EDT-only, AWT-free. The δ′ = δ equality in §3 is the right way to state a refactor's
obligation. The re-baseline warning (`SimpleEditor.java` grew 4,119 → 5,852 across
five re-derivations) is honest and should survive any re-scope.

## Verdict rationale

`rethink`, not `redirect`: the capability statement stands unchanged. What needs
reconsidering is that the issue proposes a GoF decomposition where the repo has
already proven a plan/op seam, a coverage percentage where the repo has already proven
structural ratchets, a display-lane dialog sweep where a headless registry-derived
parity test already exists, and one blocking node where five independent deliverables
sit. Each substitution makes the work smaller, lands earlier, and points at the
module-graph future in §4/§8 of `docs/grand-architecture.md` rather than at a tidier
version of the 5,852-line present.
