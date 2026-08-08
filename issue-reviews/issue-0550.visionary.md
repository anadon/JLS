# Issue #550: FEAT-C27-3: first launch lands somewhere — a starter circuit or welcome pane replaces the empty tab pane, and never regressing that is a per-commit gate
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

CAP-27 (#511) is the project's cheapest high-leverage capstone: the niche survey scored
the on-ramp 2/5 and every competitor teardown produced the same verdict — a switcher
bounces in ten minutes. #550 is PF-3, the first-run slice. Its stated goal is "first
launch never lands on an empty `JTabbedPane`."

But #550 explicitly disclaims the implementation: #381 TASK-0030 builds the empty-state
panel, and the pass-2 comment says outright "do not write an empty-state panel under this
issue's number." What #550 actually owns is three things: a gate test (AC-1), a
startup-time ratchet (AC-3), and a recorded decision (AC-4). AC-2 is #381 P9's identity
discipline restated. So the real content of this feature is *one user-facing byte of
design (which surface wins) plus two pieces of process*.

That is worth saying plainly, because it changes what "good" looks like here. Judged as a
UX feature it is nearly empty; judged as the capstone's ratchet it is defensible but
aimed at the wrong invariant. Both are fixable, and the fix makes the issue smaller and
better.

## The reframing: the empty state should be unrepresentable, not tested

`JLSStart` constructs `edits = new JTabbedPane(...)` (`src/jls/JLSStart.java:1274`) and
opens a circuit only `if (startFile != null)`. `stateChanged` already has an explicit
`ed == null` branch (`:1319`) that nulls the simulator's circuit — so the empty state is
reachable two ways: at launch, and by closing the last tab. A welcome pane covers the
first and leaves the second, and both are guarded only by a display-tagged test that can
be deleted or flake.

The elegant route is to make the state unrepresentable rather than asserted: **JLS always
has exactly one editor.** Launch creates an untitled circuit; closing the last tab
creates a fresh untitled one. `edits.getTabCount() >= 1` becomes a class invariant of
`JLSStart`, not a Swing screenshot test — and AC-1's "asserted by a display-tagged test
that fails if the launch path regresses" becomes a headless assertion on the invariant,
which is far cheaper, non-flaky, and covers the close-last-tab path the issue never
mentions. This is exactly the seam the project already prefers elsewhere
(`HeadlessCoreRatchetTest`, `ArchitectureRulesTest`, `DrawCullingParityTest`): structure
enforced, not appearance sampled.

This is also what the rivals in the survey do. Logisim-Evolution and Digital open on a
live blank canvas. A welcome pane is a wall between the user and the tool; a canvas is
the tool. Against CAP-27's own metric — *running, understood example circuit in under ten
minutes* — a modal-ish chooser costs a decision before any value is delivered.

## The finding that should reorder this whole feature

`newCircuit()` (`src/jls/JLSStart.java:2186-2209`) does this before you ever see a canvas:

```java
String name = TellUser.prompt(this, "Enter circuit name (without .jls)");
if (name == null || name.isEmpty())
        return;
if (!Util.isValidName(name)) {
        TellUser.error(JLSInfo.frame,"Invalid circuit name - must have only letters, digits & _", "Error");
        return;
}
```

A first-run user who clicks **New** on the welcome pane lands on a naming prompt with a
validation rule, and can bounce off it back to the welcome pane with nothing drawn. AC-2
wires the welcome surface to "the same shared `Action` objects the menu bar uses" — which
faithfully preserves that wall as the first thing a new user touches. The empty tab pane
is the symptom the survey noticed; the name-before-canvas prompt is the one it would have
noticed next, and it is a two-line fix (default `untitled1`, `untitled2`, …; name on
save, as every rival does).

I am explicitly disregarding the framing that AC-1 is the valuable acceptance criterion.
The highest-value byte of work reachable from this issue's premise is *delete the naming
prompt from the new-circuit path*. If PF-3 ships a beautiful welcome pane whose New
button opens a validation dialog, the capstone's ten-minute clock is not meaningfully
improved.

## AC-3: the startup ratchet measures the wrong thing

A per-commit **wall-clock** startup budget is the weakest part of this issue and the part
most likely to be regretted. There is no perf harness anywhere in `test/` today; the
nearest thing, `SizeMeasurement.java`, is deliberately named so surefire skips it. GitHub
runners are shared and noisy; the project already retries display-tagged tests twice
(`pom.xml:293`) because realization is nondeterministic, and #381 §11 records the
maintainer's own rule that "a green bar is not a user" and a test that only passes on
retry is a flake. A timing gate will either be set so loose it never fires or so tight it
fires on runner weather, and the standard response to a flaky gate is to weaken it — at
which point KC-27-1 is protected by a test that cannot fail.

KC-27-1's real content is not milliseconds; it is *the first-run surface must not do
work*. That is a structural property and can be gated deterministically:

- **No eager circuit I/O on the launch path.** Assert that starting with no `startFile`
  parses zero `.jls` files — no sample, no thumbnail, no examples index. This is the
  gate that actually matters once #548's ≥10 examples and PF-4's SVG renders exist,
  because that is where startup cost would come from.
- **The Examples list is built lazily**, populated on menu-open, not at construction.
- **A class-loading budget at first paint** (`-verbose:class` count, or a
  `ClassFileTransformer` counter) — deterministic, reproducible, and it fails naming the
  class that got dragged in. This fits the project's reproducibility culture (byte-
  reproducible jar, `.buildinfo`, `docs/reproducibility.md`) far better than a stopwatch.
- Keep a wall-clock number as a **recorded measurement** in a findings document, the way
  `docs/flatlaf-evaluation-2026-07.md` records the scaling matrix. #381 §9 already draws
  exactly this line: some observations are documents, not JUnit assertions. Dressing a
  timing measurement as a per-commit gate is the same mistake in the other direction.

## AC-4 is a false binary, and the third option is the good one

Starter circuit vs. welcome pane are both presented as the only choices. A pre-populated
starter circuit is the worse of the two under K9/D9: a first-year sees a circuit they did
not draw and cannot tell whether it is theirs, an example, or a template — that is added
conceptual load in the default view, which K9 forbids. A welcome pane is a wall.

The third option: **a blank untitled canvas plus a dismissible hint strip** carrying New /
Open Example / Tutorial, docked in the editor rather than replacing it. It delivers the
"next move" CAP-27 PF-3 asks for, is drawable-on immediately, costs one dismissible row
of conceptual load, and satisfies AC-2's identity discipline unchanged. Record that as
the decision and mark both stated options not-built — AC-4 is then satisfied by a better
answer than either candidate it enumerated.

## Trajectory: this pulls against the arc in one specific way

`ordering_after: [381]` is the problem. #381 is a very large task — a 110-literal
`Color.black` sweep across `src/jls/edit/`, a `Theme.DARK` variant with a CIE76 delta-E
bar, a manual four-platform × three-scale screenshot matrix, a FlatSVGIcon redraw, and an
n=5 usability trial — with the empty-state panel and `resources/samples/` as two
checkboxes buried inside it. Both #548 and #550 declare `ordering_after: [381]`. The
result is that CAP-27, described in #511 as "the cheapest capstone on the board," is
gated behind the most expensive unrelated mechanical sweep in the backlog.

The structural fix is not on #550, but #550 is where it becomes visible: **split #381
along the CAP-27 seam.** The empty-state panel + `resources/samples/` + README
screenshots are CAP-27 substrate and share no code with the dark-theme sweep or the
scaling matrix. Land them as their own task, let the theme work proceed on its own clock,
and CAP-27's on-ramp stops waiting on `RegisterRenderer.java`'s seventeen color literals.

Second trajectory note, already escalated in the pass-2 comment and worth restating from
this lens: the panel is now specified in five places (#73's roster, #355, #381, #511
PF-3, #550), and #550's own body plus its comment are largely about who owns what. The
coordination artifact is longer than the code. Once the invariant reframing above is
adopted, #550's residual content is a decision record and two structural assertions —
which is an acceptance criterion on #511 or a checkbox pair on the split-out #381 task,
not a feature-tier issue. Closing #550 into those is a net gain for the graph.

## What I would keep

- The instinct behind AC-1 is right: "first launch offers a next move" should be a
  permanent property, not a review item that decays. Only the enforcement mechanism is
  wrong.
- AC-2's binding to shared `Action` objects is the correct seam and matches `#75`'s
  identity discipline (`retargetEditMenus`, `test/jls/ui/EditActionMatrixTest.java`).
  Keep it — and fix what those Actions do before wiring a new surface to them.
- The deferral of "Open recent" to #76 is correctly disciplined and should stay deferred.

## Concrete restatement of this feature

1. `JLSStart` maintains the invariant that at least one editor tab exists — at launch and
   after the last close. Headless assertion, no display tag.
2. `newCircuit()` defaults to `untitledN` and validates on save, not before the canvas.
3. First run shows a dismissible hint strip (New / Open Example / Tutorial) over the live
   canvas, bound to the existing shared `Action`s; Examples populates lazily.
4. Gate: the no-`startFile` launch path parses zero circuit files and loads no more
   classes than a recorded budget. Wall-clock startup is measured and written down, not
   asserted.
5. Record the decision: starter circuit rejected on K9/D9 grounds, welcome pane rejected
   as a wall; neither is built.
