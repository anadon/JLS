# Issue #286: Themed chrome: status strip, hint bar, and toolbar buttons hardcode CYAN/yellow/WHITE, bypassing the Theme record and the FlatLaf look-and-feel
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

The stated remedy is "add chrome roles to `Theme` and route 12 sites through them."
The actual end is one sentence in the Abstract: *the editor should present one
coherent visual system*, and *dark mode must become possible*. Judged against
that end, the proposed mechanism is the wrong seam, and the cheaper route is
subtraction rather than addition. I am explicitly disregarding §14's acceptance
criteria — the 12→0 grep gate, the new `statusIdle()`/`statusActive()`/
`toolbarButton()` accessors, and "CLASSIC chrome renders exactly today's
cyan/yellow/white" — for the reasons below.

## The seam is misplaced: chrome is the look-and-feel's, not `Theme`'s

`Theme` earns its existence because no look-and-feel knows what "a wire carrying
a non-zero value" looks like. Those are *domain semantics*, and `Theme.apply()`
correctly pushes them into `JLSInfo.Palette` (`src/jls/Theme.java:147`) where the
canvas painters read them. A `JPanel`'s background is not domain semantics. It is
platform presentation, and #153 already bought a system that owns it —
`ARCHITECTURE.md`'s recorded decision installs `FlatLightLaf` by default with
`-Djls.laf=metal|system|<class>` as a supported escape hatch
(`src/jls/JLSStart.java:945`).

The issue's own §7.2 concedes the point: the new roles would be "seeded from
`UIManager` defaults." A record field that caches a `UIManager` value is not a
source of truth — it is a second copy of one, and a fragile copy:

- `Theme.DEFAULT`/`CLASSIC` are `static final`, initialized whenever `Theme` is
  first touched. `UserPrefs` restore can touch `Theme` before or after
  `installLookAndFeel()` runs; whichever `UIManager` state exists at class-init
  is frozen forever. This is a real order-of-initialization landmine the current
  design has no exposure to.
- Under `-Djls.laf=metal` or `=system`, FlatLaf-seeded chrome roles are simply
  wrong, and the CLASSIC variant would paint cyan chrome onto a native Windows
  look. #153 kept those modes deliberately; this design breaks them silently.
- It creates a *third* color-delivery mechanism: canvas → mutable `JLSInfo`
  statics; chrome → record read at widget-construction time; everything else →
  `UIManager`. Three mechanisms for one visual system is the opposite of "one
  coherent visual system."

There is also an unmentioned functional gap. Scheme switching repaints via
`JLSStart.refreshEditorColors` (`src/jls/JLSStart.java:2056`), which calls
`SimpleEditor.changeBackgroundColor` — and that method sets exactly one thing,
`ew`'s background (`src/jls/edit/SimpleEditor.java:577`). Chrome colors read at
construction would go stale on every scheme switch in an already-open tab. §8's
method list does not include extending that path, so H1 as written would not even
satisfy P2 in a live editor.

## The radically simpler route: delete, don't re-encode

Four of the twelve sites are pure decoration with no signal content:
`SimpleEditor.java:408` (`top`), `:411` (`message` construction), `:2334`
(Import button), `:2404` (every palette button). Deleting those four lines makes
chrome inherit whatever look-and-feel is installed — FlatLaf light today, Metal
under the escape hatch, and **FlatLaf dark for free**. `FlatDarkLaf` ships in the
same already-bundled jar (`pom.xml:82`), reachable through the existing
documented seam. The reframed fix is demonstrable *today*, with zero new code:
delete the literals, run `-Djls.laf=com.formdev.flatlaf.FlatDarkLaf`, screenshot.
That is a one-afternoon experiment that falsifies or confirms the whole design,
and the issue never considers it.

The remaining eight sites (`:3946` cyan-idle, `:3951`–`:3975` yellow-active) are
not a theming problem at all — they are an accessibility problem this issue
proposes to *preserve and formalize*. The hint bar already states the mode in
words: "moving element(s)", "left click to place, right click to cancel". The
background swap is a redundant, color-only channel, and #76 — this issue's own
parent — exists precisely to remove color-only semantics (WCAG 1.4.1;
`Theme`'s Okabe-Ito work and `ThemeTest`'s delta-E gate). Minting
`statusIdle()`/`statusActive()` roles so that a cyan/yellow flash can be
faithfully reproduced per-scheme, and pinning that reproduction as a DoD item, is
carrying a 2004 wart forward under a semantic name. Better: idle inherits, active
gets one L&F-derived accent (FlatLaf's `Component.accentColor` /
`FlatClientProperties`) or bold text — one styling decision, zero new roles,
correct under every L&F including dark.

Net: 12 sites → 0, no `Theme` API change, no `UIManager`-seeding helper, no
CLASSIC-chrome compatibility clause, and #289's chrome half evaporates.

## The scope is grep-shaped, not problem-shaped

P1 greps *one file* for *four constants*. That gate goes green while the same
defect, in worse form, sits untouched next door. `src/jls/edit/` holds 66
`setBackground`/`setForeground` sites; the issue names 12:

- `InteractiveSimulator.java:127-151` — `start`=green, `step`=yellow,
  `animate`=cyan, `end`=pink, `stop`=**red**, plus `bottom`=yellow (`:191`),
  `statusBar`=yellow (`:1030`), radix buttons gray/lightGray (`:244-268`). A
  red/green run-control pair is the canonical CVD failure, in the window students
  spend simulation time in, in a project whose parent feature is *color-vision-safe
  semantics*. Nineteen sites, higher user impact than all twelve here.
- `SimpleEditor.java:419-420` — `disabledBanner` at `(255,204,0)` with
  `Color.BLACK` foreground. Same failure mode, same file, same panel stack;
  excluded only because the literal is not spelled `Color.yellow`.
- `SimpleEditor.java:2000/2004` — `info.setForeground(Color.red)` / `black` for
  the error signal. Black-on-dark, and color-only again.
- `StateMachineDialog.java:301/1744/1750/1715`, `MemoryContentsDialog.java:60/73`,
  `Trace.java:123`, `ConstantDialog.java:178`, `GateDialog.java:220`.

Every one of these is a visible dark-mode break the day #289 lands. The unit of
work that actually holds is not a grep in a PR description but a **ratchet test**,
in the family this repo already runs (`HeadlessCoreRatchetTest`,
`NotificationRatchetTest`, `DialogCoverageRatchetTest`): *no Swing component under
`jls.edit` sets an absolute `Color` unless the value comes from
`JLSInfo.Palette`*, with a shrinking allowlist. That is enforceable, it covers the
simulator and the dialogs, and it cannot be reverted quietly — which P1 and P2 as
written can.

## The blocking claim is weaker than advertised

#289 declares `blocked_by: [286]` on the reasoning "chrome must follow `Theme`
roles before any dark palette can restyle it." Under the reframing, chrome
follows the *L&F*, and #289's true blocker is the one #289 already owns: the
~113 hardcoded-black canvas foregrounds, documented in `Theme`'s own class
comment (`src/jls/Theme.java:26-31`) as the reason a dark variant is absent. So
#286-as-specified buys #289 less than it claims, while #286-as-deletion buys it
the same thing in a fraction of the work. The comment's other downstream consumer,
#707 ("one Theme seam" for a print theme), is *better* served by the reframing
too: a print theme wants chrome *suppressed wholesale*, which is trivial when
chrome is L&F-owned (install a print L&F / paint the canvas only) and awkward when
chrome colors are baked into a `Theme` record that also carries canvas semantics.

## Concrete alternative design

1. **Delete** the four decorative `setBackground` calls (`:408`, `:411`, `:2334`,
   `:2404`). Chrome inherits the L&F.
2. **Replace** the eight-site cyan/yellow mode swap with one non-color-only
   affordance (accent border or bold text on active), derived from `UIManager`
   at the call site, no new `Theme` roles.
3. **Sweep** `InteractiveSimulator`, `disabledBanner`, `info.setForeground`, and
   the dialogs in the same pass — the defect is one defect.
4. **Ratchet** it: an assert-the-assertion-backed test banning absolute `Color`
   literals on Swing components outside `JLSInfo.Palette`, with a documented
   shrinking allowlist. This replaces P1 and outlives the PR.
5. **Demonstrate** dark chrome with `-Djls.laf=com.formdev.flatlaf.FlatDarkLaf`
   on the existing `gui-wayland` boot lane before #289 designs anything — the
   cheapest possible falsification of the whole approach.
6. **Record** the boundary in `ARCHITECTURE.md`: *`Theme` owns canvas semantics;
   the look-and-feel owns chrome; the user-facing "dark mode" preference switches
   both.* That single sentence prevents this issue from recurring and tells #289,
   #381, #707, and #736 which side of the line they are on.

Keep the goal. Change the mechanism, widen the surface to the defect's real
extent, and drop the CLASSIC-chrome fidelity requirement — nobody is owed a
faithful reproduction of a cyan status strip.
