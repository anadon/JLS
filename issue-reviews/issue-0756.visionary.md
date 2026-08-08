# Issue #756: TASK-C549-1: a keyboard-unreachable dialog fails the build — the reachability gate, with its seeded red run recorded first
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its acceptance criteria, #756 asks for one thing: **make the
accessibility work of #75 non-decaying**. That is the right ask, and it is the
single most characteristic move this project makes. JLS does not have a testing
culture so much as a *ratchet* culture — `HeadlessCoreRatchetTest` (no AWT in
`jls.sim`), `NotificationRatchetTest` (only `TellUser` may raise a dialog),
`PointerApiRatchetTest` (no `MouseInfo`/`getLocationOnScreen`/`getScreenSize`),
`ElementRegistryTest` totality, `PaletteContractTest`, `HotkeysHelpAccuracyTest`,
`DialogCoverageRatchetTest`, the assert-the-assertion rule in
`test/jls/ui/package-info.java`. Turning `docs/keyboard-a11y-verification.md`
from a standing checklist into a member of that family is squarely on the
project's arc, and #507's dependency graph is honest that PF-3's screen-reader
work stands on this floor. The direction: endorse.

The mechanism: no. As specified, this gate measures the one property that
cannot break here, enumerates from a registry that does not know about the
surfaces that actually break, and records its falsification in a form the
project has already outgrown. All three are fixable without changing the goal.

## Three grounded objections to the mechanism as written

**1. The chosen invariant is enforced by Swing, not by JLS — so the gate can
never move.** `grep -rn "setFocusable(false)\|FocusTraversalPolicy\|setFocusCycleRoot" src/`
returns *nothing*. There is no custom traversal policy anywhere in the tree; the
only related call is `ElementFormDialog:217`'s `setDefaultButton(ok)`. Under the
default `LayoutFocusTraversalPolicy` every visible, enabled, focusable component
is reachable from any other by construction of the toolkit. AC-1's predicate is
therefore already a theorem about AWT, not a claim about JLS. The tell is AC-2
itself: to seed a red run you must *invent* a defect the codebase has no natural
way of producing — install a bespoke traversal policy, or call
`setFocusable(false)` for the first time in the project's history. When a gate's
falsification requires synthesizing a defect that has never occurred and has no
mechanism to occur, the gate is green forever and teaches nothing.

**2. "The registry" does not contain the surfaces that decay.**
`jls.elem.ElementRegistry` is a table of *loadable element types*;
`jls.edit.ElementDialogs` maps element class → dialog. Neither knows about
`About`, `Tutorial`, `Help`, `KeyPad` (`src/jls/KeyPad.java:92`, `new JDialog(f)`),
`MemTrace`, `Trace`, `JFileChooser` (`Editor.java:104,155`), or `TellUser`'s
option panes. Worse, three real dialogs are constructed *inline inside other
dialogs* and are invisible to any type-keyed table:
`MemoryDialog.java:409` (`new JDialog(this,true)`),
`MemoryContentsDialog.java:45`, and `StateMachineDialog.java:1711`. So AC-3 —
"a newly added dialog is covered automatically" — is false as written for
exactly the population where new dialogs appear. The registry-driven walk would
be total over element creation dialogs, which are already the *best*-covered
surfaces in the tree (`DialogConstructionSmokeTest` + `DialogCoverageRatchetTest`
+ `ComponentIdentityTest` + `ElementFormDialog`'s uniform Enter/Escape/close-box
behavior), and empty everywhere else.

**3. It partly re-implements a ratchet that exists.**
`DialogCoverageRatchetTest` already is "no new dialog escapes coverage", and it
enumerates from ArchUnit bytecode — strictly stronger than a registry — with a
hand-maintained `SWEPT` list as its only weakness. #756 proposes a second
enumeration with a different backing list. Two overlapping completeness ratchets
over the same population is the kind of duplication ARCHITECTURE.md's "recorded
decisions" section exists to prevent.

## The reframing I would build instead

**A. Cut along the *construction* seam, not the enumeration seam.** JLS has
already won this argument twice. `TellUser` is the only place allowed to create
a message dialog, and a source-scan ratchet enforces it; `jls.sim` imports no
AWT, and a ratchet enforces it. Do the same for windows. `ElementFormDialog`'s
`installDialogBehavior()` is already ~80% of a universal surface contract
(default button, Escape cancels, close-box cancels, inline validation set as the
field's *accessible description*). Promote it: one `JlsWindow`/`Surfaces.open(...)`
seam that every top-level surface passes through, installing initial focus,
accessible name, and dismissal policy once; plus a `SurfaceRatchetTest` failing
on any `new JDialog(` / `new JFrame(` outside it. The surface set then becomes
**total by construction** rather than by anyone's list — the three nested dialogs
above walk into view the day they are converted, and AC-3 becomes structurally
true instead of aspirational. This is also the single highest-leverage
prerequisite for PF-3: a screen reader needs one place to hang accessible names,
initial focus and live-region hooks, and today there is no such place.

**B. Ratchet operability over *affordances*, not over focusable controls.** The
claim CAP-26 §1 step 5 actually needs is "a keyboard user can do what a mouse
user can". Note that the `EditOp` half of that is *already* total: `EditOp.accelerator`
(`src/jls/edit/EditOp.java:110-153`) throws `AssertionError` for an unmapped
constant, so a new op without a keystroke is already a build failure, and
`EditActionMatrixTest` pins one shared `Action` per op across three surfaces.
The residual is everything that is a *pointer gesture and not an `EditOp`*:
drag-to-move, drag-to-select, wire dragging, double-click-to-edit, the hand-coded
Import button (documented as the one non-`Palette` toolbar control), the
truth-table grid built from `DisplayBool`/`Cross`/`HLine`/`VLine`, the state
editor inside `StateMachineDialog`, and the canvas — which
`docs/keyboard-a11y-verification.md` explicitly defers. The precedent for
gating that population is sitting in the repo already:
**`docs/pointer-geometry-census.md` + `PointerApiRatchetTest`** — a census
document classifying every site, paired with a ratchet that makes the classified
API extinct. Build the same pair here: a *keyboard-operability census* mapping
every pointer-driven affordance to its keyboard counterpart (or an explicitly
listed manual/deferred row, which #549's third AC already demands), and a
ratchet that fails when a `MouseListener`/`MouseMotionListener` registration
appears in `jls.edit` without a census row. That gate goes red from ordinary
development, which is the only property that matters.

**C. Replace the committed transcript with a permanent negative fixture.** I am
explicitly setting aside AC-2's ordering ritual. A committed red-run transcript
proves the checker could fail once, at one commit, and then rots — and `proofs/`
currently holds a README and one Agda file, so AC-2 invents an artifact class
with no home and no decay protection. The project's own better answer is written
down in `test/jls/ui/package-info.java`: "every helper assertion is itself
pinned by at least one deliberately-failing test (assert-the-assertion, via
`assertThrows(AssertionError.class, ...)`)". Ship the seeded-unreachable surface
as a **permanent fixture in the test tree** whose meta-test asserts the checker
rejects it. Then the falsification re-runs on every build forever instead of
being archived. That is strictly stronger than what AC-2 asks for and costs less.

## What I would keep verbatim

- The outcome sentence. "Accessibility regresses one dialog at a time in every
  project without a gate" is correct and is the whole justification.
- #549's third AC (mechanizable items automated, the remainder an explicitly
  listed manual checklist, "so the gate never claims more than it tests"). That
  is the honesty rule CAP-26 §3.1 and KC-26-3 turn on, and #756 dropped it in
  the decomposition — it should come back down into the task.
- The ordering claim: this really is the floor, and under the reframing it is
  *more* load-bearing, because seam A is a prerequisite for PF-3 regardless.
- AC-4 (K9). Harmless here; a test-only change cannot move a pixel.

## Verdict

**endorse-with-reframing.** The end — a merge gate that stops a11y decay, ahead
of the screen-reader band, as #507's funded demo slice — is right and should
proceed now. But I am disregarding AC-1's registry enumeration and AC-2's
transcript-first ordering: enumerate by making the surface set total through a
single window-construction seam (the `TellUser` pattern), gate on
pointer-affordance-without-keyboard-counterpart (the pointer-census pattern)
rather than on focus-traversal reachability that Swing already guarantees, and
falsify with a permanent negative fixture rather than an archived red run. Done
that way the ratchet can actually go red on a real regression, it subsumes
`DialogCoverageRatchetTest` instead of shadowing it, and it pre-pays for PF-3.
Done as written it will be green on the day it lands and green forever after.
