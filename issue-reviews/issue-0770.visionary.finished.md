# Issue #770: TASK-C550-1: first launch never shows an empty tab pane, and a display-tagged test fails if the launch path ever regresses
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

CAP-27 (#511) scored JLS's on-ramp 2/5 and found that "a switcher from any rival bounces
in the first ten minutes without ever discovering the parts of JLS that are genuinely
superior." #770 is the task that turns AC-4 of that capstone — *first launch never lands
on an empty tab pane* — from a review item into a gate. The goal is right, it is the
cheapest capstone on the board, and it gates the value of every adoption-facing capstone
after it. Nothing below argues against doing it.

What I am arguing against is the seam it cuts along. The issue states its invariant as a
property of a Swing container (`JTabbedPane` non-empty), delegates the implementation to a
task whose critical path is a 96-literal color sweep, and demands object identity against
`Action` objects that do not exist anywhere in the application layer. Each of those is
fixable, and fixing them makes the work smaller, not larger.

## The fact the plan does not exploit: the tab pane is typed `Editor`

`JLSStart` builds the pane at `/home/user/JLS/src/jls/JLSStart.java:1274` and then treats
every child as an `Editor`. There are **nineteen unguarded `(Editor)edits.getSelectedComponent()`
casts** in that file (`:1322`, `:1369`, `:1440`, `:1461`, `:1675`, `:1808`, `:1825`, `:1842`,
`:1859`, `:1890`, `:1910`, `:1930`, `:1983`, `:2007`, `:2364`, `:2370`, `:2403`, `:2475`,
`:2987`). The first of those is inside `stateChanged`, the pane's own `ChangeListener` — it
fires on selection, on the EDT, at startup.

So the literal reading of AC-1 — put something in the tab pane so it is not empty — is the
one implementation that cannot be done safely. A welcome `JPanel` added as a tab throws
`ClassCastException` out of the change listener before the user sees it, and nineteen sites
must be audited to prevent it. The issue never mentions this, and neither does #550 or #381.
That is the strongest single argument that "the tab pane is never empty" is the wrong
sentence to build a gate out of.

Two seams avoid it entirely:

- **Starter circuit.** `setupEditor(new Circuit("untitled"), "untitled")` at the end of the
  `JLSStart` constructor when `startFile == null`. The mechanism already exists
  (`JLSStart.java:2333-2350`, the same path `newCircuit()` uses). One call. The pane is
  non-empty *with an `Editor`*, so all nineteen casts stay sound, no new component enters
  the pane, and startup cost is one empty `Circuit` — which is the honest answer to
  KC-27-1's startup-time line.
- **Overlay, not tab.** The next-move affordances live on a `JLayeredPane` over the
  editor's canvas (or as a card in `both.setTopComponent`), not as a child of `edits`.
  Real focusable buttons, real accessible names (TASK-0029's half of #355 needs that), and
  the pane stays homogeneous.

I would do both: a starter circuit so the canvas and palette are live immediately, plus an
overlay on the empty canvas offering New / Open Example / Tutorial. That combination also
covers the empty states #770 does not mention — closing the last tab (`stateChanged` has an
explicit `ed == null` branch at `:1322`) and deleting every element — where today's user
lands right back in the void the gate was written to abolish.

## The invariant is stated one level too low

"The tab pane is never empty" is an implementation fact. What CAP-27 AC-4 cares about is
"**whenever no circuit content is on screen, an actionable next move is visible.**" The
difference is not pedantic: a starter-circuit-only implementation passes the stated
criterion perfectly while the CAP-27 user still stares at a blank grid and bounces. #381 §9
and §11 warn twice that "a green bar is not a user, and the project has already mistaken
one for the other once." Writing the gate against the container rather than against the
affordance is exactly how that mistake recurs, and this time it is a *per-commit gate*, so
it will be believed for years.

## AC-2 asks for a registry that does not exist — and the catalog already names it

`grep AbstractAction src/jls/JLSStart.java` returns **nothing**. The #75 shared-`Action`
layer is `SimpleEditor.editAction(EditOp)` (`src/jls/edit/SimpleEditor.java:605`) — per
editor, editor-scoped ops. Every File-menu item is a bare `JMenuItem` plus an anonymous
`ActionListener` (`JLSStart.java:1400`, `:1412`, `:1440`, `:1461`), and Help→Tutorial is a
*submenu of three* items — Introduction, 4-Bit Counter, Full Adder (`JLSStart.java:2094-2135`)
— so "the corresponding menu item" for a single Tutorial button is not even well defined.
#381's H4 predicted this refutation; its recorded next move was "create it as a shared
`Action` and route the menu item through it too."

That is the real deliverable hiding inside #770, and the architecture has already named it.
`docs/extension-points.md` lists the seam **`app.command`** — "Command / activation trigger,
shim contract over `jls.module.Activation`, home package `jls.module`, cardinality many,
status *pending (#84, with #220's runtime)*." An application-scope command vocabulary is
what the welcome surface needs, what #548's ten-entry Examples menu needs, what the
accelerator work in #75 already wanted, and what any future command palette needs. Build
the welcome surface's three buttons as the **first three rows of `app.command`**, and
AC-2's identity assertion becomes structural rather than a test written after the fact —
the panel and the menu resolve the same command id because there is only one place a
command can come from. Hand-wire three ad-hoc `Action` fields into a 3,069-line
menu-builder instead, and #84 inherits a second informal registry to unwind.

## AC-3's conditional dissolves if there is one catalog

"Open Example resolves to #548's Examples set when present, and degrades to the #381 sample
baseline when it is not" encodes a runtime branch for what is a build-time fact: which
resources are on the classpath. `resources/` today holds only `help` and `packaging`;
`examples/` holds only `autograde`. There is no set to fall back *from*. Declare one
classpath-enumerated example catalog now (the `resources/samples/` mechanism #381 P3/P8 and
#548 both already commit to, under the #130 never-`user.dir` rule), and #548 becomes "more
rows in the same catalog, plus a menu view of it." Then there is no degradation path, no
"when present" branch, and no dead-entry failure mode to test — the problem disappears
rather than being handled.

## The gate is being placed on the flakiest substrate the project owns

`test/jls/ui/package-info.java` is explicit: layers, "the cheapest layer preferred per
assertion." `pom.xml:289` retries display-tagged tests because "popup realization is
nondeterministic," and #381 §11 says plainly that a display test which only passes on
retry is a flake, not a pass. #770 makes a full-frame `new JLSStart()` launch the project's
first *per-commit onboarding gate*. With the overlay seam, the same claim splits into two
cheaper assertions: a headless component-tree assertion that the surface exists with its
three commands bound (the `GuiConstructionObservationTest` / `ComponentIdentityTest` shape),
and a Layer-3 `RenderAssert` that the empty canvas paints its hints — both headless, both
non-retried. Keep one display-tagged smoke test on top if you like; do not make it the gate.

One more mechanical obstacle to "launches from a clean preferences state": `JLSStart` holds
`private final UserPrefs prefs = UserPrefs.open();` (`JLSStart.java:147`), and `open()`
hardcodes `Preferences.userNodeForPackage(JLSInfo.class)` (`src/jls/UserPrefs.java:68`).
The injectable constructor `UserPrefs(@Nullable Preferences node)` exists but nothing at
the application layer can reach it. As written, AC-1's test either mutates the real user
prefs node or needs a seam that does not exist yet. Add the seam; do not let a gate acquire
an OS-level side effect.

## Two of #550's four criteria quietly vanished

#770 presents itself as the capstone task for #550, but carries #550's AC-1 and AC-2 and
drops **AC-3** (the per-commit startup-time regression check, which is KC-27-1, the
capstone's own *kill criterion*) and **AC-4** (record the starter-circuit-vs-welcome-pane
decision with the K9/D9 rationale, and explicitly not build the loser). Under this lens
that is the most consequential omission in the issue: the startup-time check is the thing
that protects the project's stated arc — "the first-year must never SEE the ECE/EE
machinery" — against onboarding features accreting into launch latency. A gate that
guarantees a welcome surface exists but not that it stays free is half a gate. Either #770
carries both, or #550 needs a sibling task that does, filed at the same time.

## Ownership: the panel is trapped behind a color sweep

#770 and #550 both say the panel implementation belongs to #381 and must not be
re-specified. But #381 is a nine-checkbox mega-task whose other bullets are a 96-literal
renderer color sweep, a `Theme.DARK` variant meeting a 25 ΔE bar under simulated
dichromacy, a manual multi-OS/multi-scale screenshot matrix, a FlatSVGIcon redraw, README
screenshots, and an n=5 usability trial. The welcome panel is one line of that list. CAP-27
is described as "the cheapest capstone on the board"; routing it through the most expensive
open GUI task inverts that. The visionary move is to invert the ownership: **#770 takes the
first-run surface outright** (a `setupEditor` call, one overlay component, three command
rows), and #381 sheds that bullet and becomes what its evidence actually supports — the
theming and verification-matrix task. Both issues currently say "consumes, does not
duplicate," which today means neither one can ship the surface alone.

## What I am disregarding, and why

- **AC-1 as literally written.** I am rejecting "the tab pane is never empty" as the
  asserted invariant, in favour of "no circuit content on screen implies a visible,
  actionable next move." The stated version is unsatisfiable by the safest implementation
  and satisfiable by an implementation that does not serve CAP-27.
- **AC-2's identity target.** Identity against "the corresponding menu item" is not
  well-defined for Tutorial and not available for New or Open Example. Assert identity
  against an `app.command` id instead — the seam the catalog already declares.
- **AC-3's conditional.** One catalog, no fallback branch.

## If only one thing changes

Cut the seam at `app.command`, not at `JTabbedPane`. Everything else in this issue — the
gate, the identity discipline, the refusal to smuggle in "Open recent" — survives intact
and gets cheaper.
