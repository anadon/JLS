# Issue #507: CAP-26: a colorblind student and a blind student complete the same JLS lab — one through a verified palette with redundant encoding, one through spoken navigation, prose narrative and a swell-paper tactile export
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## The claim, and whether it is the right one

The end state — a blind student completes the same lab as everyone else, and a
colorblind student never depends on hue — is the right ambition and it is
genuinely category-defining. Nothing below argues with the outcome. What I am
arguing with is the route: CAP-26 cuts along the *canvas* seam (make the
painted picture speak) when the project's whole trajectory already points at a
different seam (make the circuit's *meaning* a first-class artifact, of which
the picture is one rendering). Cut there instead and four of the six planned
features collapse into one model plus thin views, the named blow-out risk
(KC-26-2) stops being architectural, and the result works on Windows and macOS
rather than only inside a Linux Orca rig.

I am also explicitly disregarding two stated acceptance criteria: AC-5's
pixel-identity clause and AC-2's Orca-session-in-CI. Reasons in §4 and §5.

## 1. The reframing: one non-visual circuit model, four consumers

JLS already builds a language-neutral structural model of a drawn circuit:
`HdlExporter.buildModel` → `src/jls/hdl/HdlModel.java` (1005 lines: ports, nets,
per-element statements, deterministic legalized names, a `renames` map), fed to
pluggable emitters (`VerilogEmitter`, `VhdlEmitter`). That is 90% of "what is
in this circuit and what connects to what", already headless, already
deterministic, already tested.

Build **one circuit-outline model** on that walk, and derive:

1. **An outline view** — a stock `JTree`/`JTable` docked beside the canvas:
   elements, ports, nets, live values. Because it is stock Swing it is
   accessible *for free* on Windows (Access Bridge), macOS (Cocoa peer) and
   Linux (AT-SPI), with no per-element accessible-proxy layer and no live-region
   trickery. Revised 508 **E101.2 Equivalent Facilitation** is exactly the
   provision that makes this a conforming answer rather than a dodge.
2. **The prose narrative (PF-4a)** — a third emitter over the same walk, sibling
   to the two HDL emitters. AC-6's byte-identical determinism then comes for
   free from machinery `SvgExportTest`/`DeterministicSaveTest` already pin.
3. **Live signal state (PF-3's hard half)** — a value column that updates on the
   EDT. A `JTable` cell change is an accessible-value change every bridge
   already ships support for. KC-26-2 ("can Swing deliver live announcements at
   all?") is a question about custom-painted canvases; it does not arise here.
4. **The VPAT's evidence for 1.1.1 / 1.3.1 / 4.1.2** — the criteria whose
   current answer is "the canvas is opaque" become "an equivalent accessible
   representation of the same information is provided."

This is not my invention: `docs/standards-adoption/03-accessibility-conformance.md`
(in-tree, §6) costs the full JAAPI canvas scene model at **8–15 maintainer-days
plus permanent per-element maintenance plus a genuine risk java-atk-wrapper
surfaces none of it**, and recommends the outline view at 3–4 days instead —
"gate the full JAAPI canvas tree on an actual user request." CAP-26 funds the
rejected option (PF-3, 5–8 mw, its own named risk band) and never mentions the
recommended one. That is the single biggest divergence between this capstone
and the project's recorded thinking.

The outline model also pays outside accessibility, which is how you can tell it
is the right seam: it is what a circuit diff wants (#170 collab), what an
autograder or an LLM reading a `.jls` wants, what an instructor writing a lab
key wants, and it is the same "structural model + emitters" shape the HDL work
already committed to. An `AccessibleContext` proxy layer over the canvas serves
exactly one consumer and adds a seventeenth entry to ARCHITECTURE.md's
sixteen-step "adding an element today" list, forever.

## 2. PF-1 is mostly already shipped; retarget it

`src/jls/edit/WireRenderer.java:41-54` (`strokeFor`) already encodes wire state
in a **non-color channel**: HiZ dashed, non-zero thick, zero thin — plus an open
ring glyph on a touching wire end — and `test/jls/elem/WireValueChannelTest.java`
proves by rendering that each state leaves a different footprint *independent of
the colors chosen*. The capstone's Background asserts "there is no redundant
non-color encoding". Its falsification grep (`tritanop|vpat|tactile|swell`)
could not have found this, so the absence claim is an artifact of the search
terms, not of the tree.

Retarget PF-1 to the actual delta, which is small: add tritanopia to
`ThemeTest`'s ΔE≥25 ratchet (a third simulation matrix, lines not weeks); build
the grayscale/CVD screenshot oracle; and extend the second channel to the states
that *have* none — `watch`, `highlight`, `initialState`, bus values, error. The
2–3 mw demo slice is days, not weeks, and AC-1 gets its apparatus almost
immediately.

## 3. The prerequisite nobody owns: the Windows installers ship no Access Bridge

`scripts/build-installer.sh:145` derives the jlink module set from
`jdeps --print-module-deps`. `jdeps` reports *static* dependencies, so
`jdk.accessibility` — the module carrying the Java Access Bridge — can never
appear. I grepped `scripts/` and `.github/`: `jdk.accessibility` and
`accessibility.properties` appear **nowhere**. Every `.msi` this project
publishes therefore bundles a runtime through which **NVDA and JAWS receive
nothing at all**, no matter what PF-3 builds. A GitHub search finds no open
issue mentioning it, and none of #542–#549 covers it.

So CAP-26's §1 step 2 ("a blind student builds a two-gate circuit by keyboard
with spoken feedback") is, on the project's flagship distribution, false at the
end of the capstone as scoped — and PF-5's generated VPAT would have to rate
508 §502.3.\*, EN 11.5.2.\* and WCAG 4.1.2 **Does Not Support** for Windows
while six features' worth of work sits behind a bridge that is not loaded. This
is a one-line module addition plus a properties write in the same build step as
`clamp_mtimes`, pinned by a source-grep test in the house
`KeyPadAccessibilityPinTest` style. It belongs at the front of this capstone,
not absent from it.

## 4. AC-5's pixel-identity gate contradicts PF-5

K9/KC-26-4 gates every PF-1 commit on the default theme being *pixel-unchanged*.
But `Theme.DEFAULT` provably fails WCAG **1.4.11 Non-text Contrast** today:
`nonZero` `#E69F00` = 2.10:1, `watch` `#56B4E9` = 2.31:1, and the #75 keyboard
caret is painted in `selectionColor` `(240,240,240)` on white
(`SimpleEditor.java:2509`) = **1.14:1** — the focus indicator of the keyboard
feature the whole blind-lab path stands on is invisible. Pixel identity forbids
the fix; PF-5 then has to publish the failure. And shipping the fix only as a
non-default theme is the exact move the in-tree playbook rules out ("1.4.11 is
judged on what the user gets out of the box").

Re-derive K9 as what it actually protects: **`Theme.CLASSIC` stays
byte-identical** (that is the real promise to existing users, already pinned by
`ThemeTest.classicReproducesTheLegacyPalette`), no state ever silently changes
meaning, and every default-palette change lands in `CHANGELOG.md`. Pixel
identity of `DEFAULT` is not that promise; it is a proxy that here forbids the
goal.

## 5. Verify the accessible tree, not the screen reader

AC-2 puts a scripted Orca session in CI on the #101 Wayland rig. The same
in-tree playbook recommends **against** exactly this: it needs a session D-Bus,
GNOME GSettings schemas, and java-atk-wrapper, and the `gui-wayland` lane's own
history (twenty runs to earn promotion, an `UNVERIFIED-PLACEHOLDER` checksum,
`PIXEL_DIFF_MIN` still at 0) is the honest cost estimate. A lane that is red for
substrate reasons erodes the green build that every other ratchet depends on.

Stronger evidence for less flake: a **golden accessible-tree dump** (depth,
name, role, focusable, value) compared byte-exact, run under the existing
`@Tag("display")` Xvfb execution — it is simultaneously the regression guard and
the appendix a procurement reviewer can read — plus a dated per-release **manual
AT checklist** across Orca/NVDA/VoiceOver, modeled on
`docs/wayland-desktop-checklist.md`. That covers the two platforms Orca-in-CI
will never reach, and it keeps PF-5's honesty rule intact: no row rated above
"Not Evaluated" for 502.3/11.5.2/4.1.2 without a dated AT session.

Corollary for PF-2: put the CVD simulation transform in AWT-free color math so
the *same* function serves the in-app framebuffer preview and the AC-1
screenshot assertions — then AC-1 runs in the plain headless surefire lane, not
the fragile display one. One filter, two consumers, no new CI lane.

## 6. Tactile export is a render profile, not a pipeline

`CircuitRenderer` already emits SVG via JFreeSVG and hands back
`svg.getSVGDocument()` (`:355`) — a DOM you can post-process deterministically.
"Tactile" is then a **presentation profile** of the existing renderer (stroke
widths, spacing, symbol substitutions) plus `<title>`/`<desc>` injection, not a
second export path. Make the profile registry the deliverable: one
state → (color, stroke, glyph) mapping, registry-keyed with a totality test,
with profiles for screen / dark / print (CAP-24's symbols) / CVD-preview /
tactile. Risk 3.4 already sees that this seam must exist; promote it from a risk
note to PF-1's actual product, and CAP-24's print symbols, #355's DARK variant,
and the 126 hardcoded-black call sites all get funded by the same sweep.

## 7. Duplication and ordering

- **PF-3 vs #355 TASK-0029 / #380.** #544 says it "extends, does not re-own"
  #355's canvas `AccessibleContext`. But static reporting and traversal are two
  halves of one object graph: that is a seam drawn through the middle of a
  class, and it will be renegotiated at implementation time. Under the outline
  reframing the overlap dissolves — CAP-26 owns the outline model and its views
  and never touches the canvas; #355 keeps the canvas scene model if it is ever
  wanted.
- **Fund the provable failures before the report about them.** Today, un-owned
  by any of #542–#549: the Access Bridge module (§3); the three 1.4.11 contrast
  defects (§4); `resources/help/**` — 83 files, 10 `<img>`, **zero** `alt`,
  **zero** `lang` (straight 1.1.1 and 3.1.1 Level A failures in the product
  documentation, in scope as EN clause 10 / 508 602.2); and `Trace.java:20`,
  626 lines of mouse-only `JPanel` with no key bindings and no accessible
  wiring — a **Level A 2.1.1** failure sitting inside the simulator the lab path
  runs through. Each of these is days. A VPAT generated before they are fixed is
  honest and embarrassing; generated after, it is the document the capstone
  wants.
- **The i18n note in risk 3.5 understates itself.** A generated prose narrative
  is the first artifact whose *entire content* is English sentences, which is a
  far larger surface than the dialog strings the non-goal was recorded against —
  and, being generated, the cheapest place to make translation possible later.
  Template the sentences against a phrase table; never concatenate. That costs
  nothing now and keeps ARCHITECTURE.md's revisit trigger honest.

## Reordered spine (what I would fund, in order)

0. `jdk.accessibility` + `accessibility.properties` in the installer, pinned.
1. The three contrast fixes: dedicated `focus` role, visible caret, canvas focus
   ring, palette recolor jointly against ΔE≥25 — with `ThemeContrastTest`.
2. Help `alt`/`lang` sweep + an accessibility help page (508 §602.3 / EN 12.1.1).
3. Trace-window keyboard operability.
4. **The circuit outline model + outline view** — the capstone's real centre.
5. Prose narrative as a third emitter; tactile SVG as a render profile.
6. Tritanopia + CVD filter (headless) + grayscale oracle; operability ratchet.
7. VPAT generator last, mechanical, over a product whose Level A defects are
   closed.

Steps 0–3 are roughly a week and move more students than PF-3 as filed. Step 4
is where the category-defining claim actually lives. PF-6 and PF-5 are endorsed
as written; PF-2 is endorsed with the headless-filter note; PF-1 shrinks to its
real delta; PF-3 and PF-4 are the two I would re-cut along the outline seam.
